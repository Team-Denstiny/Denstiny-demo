package com.example.oauth2.service;

import com.example.oauth2.dto.*;
import com.example.user.UserEntity;
import com.example.user.UserRepository;
import com.example.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("로그인한 유저 {}",oAuth2User);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("로그인한 사이트 {}",registrationId);

        OAuth2Response oAuth2Response = null;
        switch (registrationId) {
            case "naver":
                oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
                break;
            case "kakao":
                oAuth2Response = new KakaoResponse(oAuth2User.getAttributes());
                break;
            default:
                oAuth2Response = null;
                break;
        }
        log.info("response {}",oAuth2Response);

        String resourceName = oAuth2Response.getProvider();
        String resourceId = oAuth2Response.getProviderId();

        // db에 oauth2 로그인 정보 저장
        UserEntity existData = userRepository.findByResourceId(resourceId);

        if (existData == null){

            // 유니크한 닉네임 저장하는 로직
            String uniqueNickname = getUniqueNickname(oAuth2Response.getNickname());

            UserEntity userEntity = UserEntity.builder()
                    .name(oAuth2Response.getName())
                    .nickName(uniqueNickname)
                    .birthAt(oAuth2Response.getBirthyear())
                    .email(oAuth2Response.getEmail())
                    .role(UserRole.ROLE_MEMBER)
                    .phoneNumber(oAuth2Response.getPhone())
//                    .address()
                    .resourceName(resourceName)
                    .resourceId(resourceId)
                    .profileImg(oAuth2Response.getProfile())
                    .build();

            // 저장
            userRepository.save(userEntity);

            UserDTO userDTO = UserDTO.builder()
                    .resourceName(resourceName)
                    .resourceId(resourceId)
                    .role(UserRole.ROLE_MEMBER.toString())
                    .name(oAuth2Response.getName())
                    .nickname(oAuth2Response.getNickname())
                    .email(oAuth2Response.getEmail())
                    .birthAt(oAuth2Response.getBirthyear())
                    .phoneNumber(oAuth2Response.getPhone())
                    .profileImg(oAuth2Response.getProfile())
                    .isNewUser(true)
                    .build();

            return new CustomOAuth2User(userDTO);

        }
        else {

            existData.setEmail(oAuth2Response.getEmail());
            existData.setName(oAuth2Response.getName());
            existData.setNickName(oAuth2Response.getNickname());
            existData.setPhoneNumber(oAuth2Response.getPhone());
            existData.setProfileImg(oAuth2Response.getProfile());

            userRepository.save(existData);

            UserDTO userDTO = UserDTO.builder()
                    .resourceName(existData.getResourceName())
                    .resourceId(existData.getResourceId())
                    .role(UserRole.ROLE_MEMBER.toString())
                    .name(oAuth2Response.getName())
                    .email(oAuth2Response.getEmail())
                    .birthAt(existData.getBirthAt())
                    .phoneNumber(oAuth2Response.getPhone())
                    .profileImg(oAuth2Response.getProfile())
                    .isNewUser(false)
                    .build();

            return new CustomOAuth2User(userDTO);
        }

    }

    private String getUniqueNickname(String baseNickname) {
        String uniqueNickname = baseNickname;
        int count = 1;
        while (userRepository.existsByNickName(uniqueNickname)) {
            uniqueNickname = baseNickname + count;
            count++;
        }
        return uniqueNickname;
    }


}
