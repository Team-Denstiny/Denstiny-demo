package com.example.jwt;

import static com.example.constant.TokenHeaderConstant.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.domain.user.controller.model.CustomUserDetails;
import com.example.domain.user.controller.model.UserLoginRequest;
import com.example.domain.user.controller.model.UserLoginResponse;
import com.example.refresh.RefreshEntity;
import com.example.refresh.RefreshRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import api.Api;
import api.Result;
import error.ErrorCode;
import exception.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil,RefreshRepository refreshRepository) {
        super.setAuthenticationManager(authenticationManager);
        setFilterProcessesUrl("/api/users/login"); // 커스텀 로그인 경로 설정 (spring security 기본 경로는 /login)

        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshRepository = refreshRepository;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        try {
            // 요청 바디에서 JSON 데이터를 읽어와서 UserLoginRequest 객체로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            UserLoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), UserLoginRequest.class);

            System.out.println(loginRequest);

            // UserLoginRequest 객체에서 이메일과 비밀번호 추출
            String email = loginRequest.getEmail();
            String password = loginRequest.getPassword();

            // UsernamePasswordAuthenticationToken 생성
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);

            return authenticationManager.authenticate(authToken);
        } catch (IOException e) {
            throw new ApiException(ErrorCode.SERVER_ERROR, "login 요청시 parsing 실패");
        }
    }
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {

        //유저 정보
        String email = authentication.getName();

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String resourceId = userDetails.getResourceId();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        //토큰 생성
        String access = jwtUtil.createJwt("access", resourceId, role, 600000L);
        String refresh = jwtUtil.createJwt("refresh", resourceId,role,86400000L );

        // refreshToken RDB에 저장
        // TODO RDB <--> REDIS
        addRefreshEntity(resourceId, refresh,86400000L);

        // 헤더와 쿠키 설정
        response.setHeader(HEADER_AUTHORIZATION, access);
        response.addCookie(createCookie("refresh", refresh));


        // JSON 응답 설정
        response.setContentType("application/json; charset=UTF-8");

        Result result = new Result(200, "로그인 성공", "성공");
        UserLoginResponse loginUser = UserLoginResponse.builder().id(userId).build();

        Api<UserLoginResponse> apiResponse = new Api<>(result, loginUser);

        // JSON 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);


        // 응답 작성
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();


        // 로그인 성공
        log.info("로그인에 성공하였습니다. 이메일 : {}", email);
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24*60*60);
        //cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        log.info("쿠키Path 추적(in createCoookie) : {}", cookie.getPath());

        return cookie;
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {

        // JSON 응답 설정
        response.setContentType("application/json; charset=UTF-8");

        // 실패 응답 메시지
        Result result = new Result(401, "로그인 실패", "인증에 실패하였습니다.");
        ResponseEntity<Result> responseEntity = new ResponseEntity<>(result, HttpStatus.UNAUTHORIZED);

        // JSON 응답을 문자열로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(responseEntity.getBody());

        // 응답 작성
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
        // 로그인 실패
        log.info("로그인에 실패하였습니다");
    }

    private void addRefreshEntity(String resourceId, String refresh, Long expiredMs) {

        Date date = new Date(System.currentTimeMillis() + expiredMs);

        RefreshEntity refreshEntity = RefreshEntity.builder()
                .resourceId(resourceId)
                .refresh(refresh)
                .expiration(date.toString())
                .build();

        refreshRepository.save(refreshEntity);
    }
}
