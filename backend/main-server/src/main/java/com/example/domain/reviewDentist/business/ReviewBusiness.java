package com.example.domain.reviewDentist.business;

import annotation.Business;
import com.example.domain.reviewDentist.controller.model.ReviewResponse;
import com.example.domain.reviewDentist.controller.model.ReviewRequest;
import com.example.domain.reviewDentist.converter.ReviewConverter;
import com.example.domain.reviewDentist.service.ReviewInfoService;
import com.example.domain.reviewDentist.service.ReviewService;
import com.example.domain.user.service.UserService;
import com.example.error.UserErrorCode;
import com.example.reviewDentist.Document.ReviewDoc;
import com.example.reviewDentist.Document.ReviewInfoDoc;
import com.example.user.UserEntity;
import error.ErrorCode;
import exception.ApiException;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Business
@AllArgsConstructor
@Transactional
public class ReviewBusiness {

    private final ReviewInfoService reviewInfoService;
    private final ReviewService reviewService;
    private final ReviewConverter reviewConverter;
    private final UserService userService;

    public String addReview(Long id, String dentistId, ReviewRequest reviewRequest) {

        // 로그인한 유저와, 리뷰를 적는 유저가 같은지 확인하는 로직
        if (!reviewRequest.getUserId().equals(id)){
            throw new ApiException(UserErrorCode.USER_NOT_AUTHORIZED, "로그인 한 유저와 추가하는 리뷰의 유저가 다릅니다!!!");
        }

        ReviewDoc reviewDoc = reviewConverter.toReviewDoc(reviewRequest);
        reviewDoc.setHospitalId(dentistId);
        reviewDoc.setDate(LocalDateTime.now());
        reviewDoc.setDepth(1);
        reviewService.saveReview(reviewDoc);


        ReviewInfoDoc reviewInfoDocById = reviewInfoService.findReviewInfoDocById(dentistId);
        reviewInfoDocById.getReviews().add(reviewDoc.getId());
        reviewInfoService.saveReviewInfoDoc(reviewInfoDocById);

        return dentistId + "에 리뷰가 추가되었습니다.";
    }


    public String deleteReview(Long id, String dentistId, String reviewId){

        if (!reviewService.isReviewExist(reviewId)){
            throw new ApiException(ErrorCode.NULL_POINT,"해당 리뷰는 없는 리뷰입니다");
        }

        ReviewDoc reviewDoc = reviewService.findReviewById(reviewId);

        if (!reviewDoc.getUserId().equals(id)) {
            throw new ApiException(UserErrorCode.USER_NOT_AUTHORIZED, "자신이 작성한 리뷰만 삭제할 수 있습니다!!!");
        }

        // 대댓글이 있으면 먼저 삭제
        deleteReplies(reviewDoc);

        // 그 다음 댓글 삭제
        reviewService.deleteById(reviewId);

        // 검색을 위한 reviewInfoDoc의 reviewId(ObjectId)를 삭제
        reviewInfoService.deleteReviews(dentistId,reviewId);

        return dentistId + "의 " + reviewId + "번 리뷰가 성공적으로 삭제되었습니다.";
    }

    // 대댓글 삭제 로직
    private void deleteReplies(ReviewDoc parentReview) {
        for (ObjectId commentId : parentReview.getCommentReplys()) {
            // 대댓글을 삭제
            reviewService.deleteById(commentId.toHexString());
        }
    }

    public String updateReview(Long id, String dentistId, String reviewId, ReviewRequest reviewRequest){

        if (!reviewService.isReviewExist(reviewId)){
            throw new ApiException(ErrorCode.NULL_POINT,"해당 리뷰는 없는 리뷰입니다");
        }

        ReviewDoc reviewDoc = reviewService.findReviewById(reviewId);

        if (!reviewRequest.getUserId().equals(id)) {
            throw new ApiException(UserErrorCode.USER_NOT_AUTHORIZED, "자신이 작성한 리뷰만 수정할 수 있습니다!!!");
        }

        // 내용 업데이트
        reviewDoc.setDate(LocalDateTime.now());
        reviewDoc.setContent(reviewRequest.getContent());

        reviewService.saveReview(reviewDoc);

        return dentistId + "의 " + reviewId + "번 리뷰가 성공적으로 수정되었습니다.";
    }

    /**
     * 병원id를 가지고 -> 리뷰들 조회하기 !
     */
    public List<ReviewResponse> findReviewsByDentist(String dentistId){

        ReviewInfoDoc reviewInfoDocById = reviewInfoService.findReviewInfoDocById(dentistId);

        List<ReviewResponse> reviewResponses = reviewInfoDocById.getReviews()
                .stream()
                .map(objectId -> {
                    ReviewDoc reviewDoc = reviewService.findReviewByObjectId(objectId);
                    ReviewResponse reviewResponse = reviewConverter.toReviewResponse(reviewDoc);

                    // nickname, img는 변경 가능성이 있으므로 -> 조회시마다 가져온다
                    UserEntity userById = userService.getUserById(reviewDoc.getUserId());
                    reviewResponse.setNickName(userById.getNickName());
                    reviewResponse.setImageUrl(userById.getProfileImg());

                    return reviewResponse;
                }).collect(Collectors.toList());

        return reviewResponses;
    }
    /**
     * 개인이 쓴 모든 리뷰들 조회하기 !
     */
    public List<ReviewResponse> findReviewsByUser(Long userId) {

        // 사용자 ID로 리뷰 찾기
        List<ReviewDoc> reviewDocList = reviewService.findReviewByUserId(userId);
        return reviewDocList.stream()
                .map(reviewDoc -> {
                    ReviewResponse reviewResponse = reviewConverter.toReviewResponse(reviewDoc);

                    // nickname, img는 변경 가능성이 있으므로 -> 조회시마다 가져온다
                    UserEntity userById = userService.getUserById(reviewDoc.getUserId());
                    reviewResponse.setNickName(userById.getNickName());
                    reviewResponse.setImageUrl(userById.getProfileImg());
                    return reviewResponse;

                })
                .collect(Collectors.toList());
    }
    /**
     * 개인이 해당 병원에 적은 모둔 리뷰들 조회하기
     */
    public List<ReviewResponse> findReviewsByUserAndHospital(Long userId,String hospitalId) {
        List<ReviewDoc> reviewDocList = reviewService.findReviewByUserIdAndHospitalId(userId, hospitalId);
        return reviewDocList.stream()
                .map(reviewDoc -> {
                    ReviewResponse reviewResponse = reviewConverter.toReviewResponse(reviewDoc);

                    // nickname, img는 변경 가능성이 있으므로 -> 조회시마다 가져온다
                    UserEntity userById = userService.getUserById(reviewDoc.getUserId());
                    reviewResponse.setNickName(userById.getNickName());
                    reviewResponse.setImageUrl(userById.getProfileImg());

                    return reviewResponse;
                })
                .collect(Collectors.toList());
    }
}
