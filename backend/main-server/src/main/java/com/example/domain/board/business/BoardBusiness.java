package com.example.domain.board.business;

import annotation.Business;
import com.example.board.BoardEntity;
import com.example.board_image.BoardImageEntity;
import com.example.domain.board.controller.model.BoardGetBoardsResponse;
import com.example.domain.board.service.BoardImageService;
import com.example.domain.board.service.BoardService;
import com.example.domain.board.converter.BoardConverter;
import com.example.domain.board_comment.service.BoardCommentService;
import com.example.domain.heart.service.HeartService;
import com.example.domain.user.service.UserService;
import com.example.user.UserEntity;
import com.example.util.BoardImageUtil;
import com.example.domain.board.controller.model.BoardAddRequest;
import com.example.domain.board.controller.model.BoardAddResponse;
import error.ErrorCode;
import exception.ApiException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;


@Business
@AllArgsConstructor
public class BoardBusiness {
    private final BoardService boardService;
    private final BoardCommentService boardCommentService;
    private final BoardImageService boardImageService;
    private final BoardConverter boardConverter;
    private final HeartService heartService;
    private final UserService userService;

    // 게시글 생성
    public BoardAddResponse addBoard(BoardAddRequest boardReq, List<MultipartFile> images, Long userId){
        BoardEntity board = Optional.ofNullable(boardReq)
                .map(it -> boardConverter.toBoardEntity(it, userId))
                .map(it -> boardService.addBoard(it))
                .orElseThrow(() -> new ApiException(ErrorCode.NULL_POINT, "addBoard null point"));

        // findById 시 Optional 처리
        // BoardEntity boardId = boardService.getReferenceBoardId(board.getBoardId()).orElseThrow(() -> new ApiException(ErrorCode.NULL_POINT, "Board Id null point"));
        try {
            List<BoardImageEntity> list = BoardImageUtil.parseFileInfo(boardService.getReferenceBoardId(board.getBoardId()), images);

            if (list.isEmpty()){
            }
            else{
                List<BoardImageEntity> pictureBeans = new ArrayList<>();
                for (BoardImageEntity boardImage : list) {
                    pictureBeans.add(boardImageService.addBoardImage(boardImage));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        if(boardReq.getCategory() == 1 && ObjectUtils.isEmpty(boardReq.getDentalType())) {
//            Optional.ofNullable(boardReq.getDentalType())
//                    .map(it -> boardDentalTypeConverter.toBoardDentalTypeEntity(boardService.getReferenceBoardId(board.getBoardId()), it))
//                    .map(it -> boardDentalTypeService.addBoardDentalType(it))
//                    .orElseThrow(() -> new ApiException(ErrorCode.NULL_POINT, "Dental Type is null"));
//            }
//        }

        return boardConverter.toBoardAddResponse(board);
    }

    // 게시글 삭제
    public String deleteBoard(Long userId, Long boardId) {
        // getReferenceBoardId로 Board 엔티티 가져옴
        BoardEntity board = boardService.getReferenceBoardId(boardId);

        // 조회한 board의 writer가 userId와 같은지 비교
        if(board.getWriter().equals(userId)) {
            // writer와 userId가 같으면 게시글 삭제
            boardService.deleteBoard(board);
            return "성공적으로 유저" + userId + "이 게시글 번호" + boardId + "게시글을 삭제하였습니다.";
        }
        return "유저" + userId + "이 게시글 번호" + boardId + "게시글 삭제에 실패하였습니다.";
    }

    // 게시글 조회 - 내가 쓴 글
    public Page<BoardGetBoardsResponse> findMyBoards(Long userId, Integer page, Integer size) {
        Page<BoardEntity> boardEntities = boardService.findByWriter(userId, PageRequest.of(page, size));

        // BoardEntity를 BoardGetBoardsResponse로 변환
        List<BoardGetBoardsResponse> responses = boardEntities.stream()
                .map(board -> boardConverter.toBoardGetMyBoardsResponse2(board, userId))
                .collect(Collectors.toList());

        // 변환된 리스트와 페이지 정보를 사용하여 PageImpl 생성
        return new PageImpl<>(responses, boardEntities.getPageable(), boardEntities.getTotalElements());
    }

    // 게시글 조회 - 내가 좋아요 한 글
    public List<BoardGetBoardsResponse> findMyHeartBoards(Long userId) {
        UserEntity user = userService.getReferenceUserId(userId);

        return heartService.findByUser(user).stream()
                .map(heart -> {
                    BoardGetBoardsResponse boardGetBoardsResponse = boardConverter.toBoardGetMyBoardsResponse2(heart.getBoard(), userId);
                    return boardGetBoardsResponse;
                }).collect(Collectors.toList());
    }

    // 게시글 조회
    public Page<BoardGetBoardsResponse> findBoardsByCategory(Long userId, Long category, Integer page, Integer size) {
        if (category == 0) {
            Page<BoardEntity> boardEntities = boardService.findAll(PageRequest.of(page, size));

            // BoardEntity를 BoardGetBoardsResponse로 변환
            List<BoardGetBoardsResponse> responses = boardEntities.stream()
                    .map(board -> boardConverter.toBoardGetMyBoardsResponse2(board, userId))
                    .collect(Collectors.toList());

            // 변환된 리스트와 페이지 정보를 사용하여 PageImpl 생성
            return new PageImpl<>(responses, boardEntities.getPageable(), boardEntities.getTotalElements());
        } else {
            Page<BoardEntity> boardEntities = boardService.findByCategory(category, PageRequest.of(page, size));

            // BoardEntity를 BoardGetBoardsResponse로 변환
            List<BoardGetBoardsResponse> responses = boardEntities.stream()
                    .map(board -> boardConverter.toBoardGetMyBoardsResponse2(board, userId))
                    .collect(Collectors.toList());

            // 변환된 리스트와 페이지 정보를 사용하여 PageImpl 생성
            return new PageImpl<>(responses, boardEntities.getPageable(), boardEntities.getTotalElements());
        }
    }
}
