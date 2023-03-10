package dev.shlee.matzip.services;

import dev.shlee.matzip.entities.data.PlaceEntity;
import dev.shlee.matzip.entities.data.ReviewEntity;
import dev.shlee.matzip.entities.data.ReviewImageEntity;
import dev.shlee.matzip.entities.member.UserEntity;
import dev.shlee.matzip.enums.data.AddReviewResult;
import dev.shlee.matzip.exceptions.RollbackException;
import dev.shlee.matzip.interfaces.IResult;
import dev.shlee.matzip.mappers.IDataMapper;
import dev.shlee.matzip.vos.PlaceVo;
import dev.shlee.matzip.vos.ReviewVo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;

@Service(value = "dev.shlee.matzip.services.DataService")
public class DataService {

    private final IDataMapper dataMapper;

    public DataService(IDataMapper dataMapper) {
        this.dataMapper = dataMapper;
    }

    public PlaceVo[] getPlaces(double minLat, double minLng, double maxLat, double maxLng) {
        return this.dataMapper.selectPlacesExceptImage(minLat, minLng, maxLat, maxLng);
    }

    public PlaceEntity getPlace(int index) {
        return this.dataMapper.selectPlaceByIndex(index);
    }

    public Enum<? extends IResult> addReview(UserEntity user, ReviewEntity review, MultipartFile[] images) throws RollbackException, IOException {
        if (user == null) {
            return AddReviewResult.NOT_SIGNED;
        }
        review.setUserId(user.getId());
        if (this.dataMapper.insertReview(review) == 0) {
            return AddReviewResult.FAILURE;
        }
        if (images != null && images.length > 0) {
            for (MultipartFile image : images) {
                ReviewImageEntity reviewImage = new ReviewImageEntity();
                reviewImage.setReviewIndex(review.getIndex());
                reviewImage.setData(image.getBytes());
                reviewImage.setType(image.getContentType());
                if (this.dataMapper.insertReviewImage(reviewImage) == 0) {
                    throw new RollbackException();
                }
            }
        }
        return AddReviewResult.SUCCESS;
    }

    //?????? ????????????
    public ReviewVo[] getReviews(int placeIndex) {
        ReviewVo[] reviews = this.dataMapper.selectReviewsByPlaceIndex(placeIndex);
        for (ReviewVo review : reviews) {
            ReviewImageEntity[] reviewImages = this.dataMapper.selectReviewImagesByReviewIndexExceptData(review.getIndex());

            int[] reviewImageIndexes = Arrays.stream(reviewImages).mapToInt(ReviewImageEntity::getIndex).toArray();
            // ?????? ????????? ?????? ?????? ????????? ??????
            // int[] reviewImageIndexes = new int[reviewImages.length];
            // for (int i=0 ; i < reviewImages.length ; i++){
            // reviewImageIndexes[i] = reviewImages[i].getIndex();
            // }

            review.setImageIndexes(reviewImageIndexes);
        }
        return reviews;
    }

    public ReviewImageEntity getReviewImage(int index){
        return this.dataMapper.selectReviewImageByIndex(index);

    }

    public Enum<? extends IResult> deleteReview(ReviewVo review, UserEntity user) {
        ReviewVo existingReview = this.dataMapper.selectReviewsByReviewIndex(review.getIndex());
        if (existingReview == null) {
            return AddReviewResult.FAILURE;
        }

        if (user == null || !user.getId().equals(existingReview.getUserId())) {
            //?????? ????????? ????????? ?????? ????????? ?????????????????? -> ||
            //?????? ????????? ???????????? ???????????? ???????????? ????????? ????????? ????????? ?????????
            return AddReviewResult.FAILURE;
        }

        review.setIndex(existingReview.getIndex());
        // ????????? ???????????? ????????? ???????????? ?????????????????????
        // BoardId ??????????????? -> ?????? Controller??? ???

        System.out.println(review.getIndex());

        return this.dataMapper.deleteReviewByIndex(review.getIndex()) > 0
                ? AddReviewResult.SUCCESS
                : AddReviewResult.FAILURE;
    }
}