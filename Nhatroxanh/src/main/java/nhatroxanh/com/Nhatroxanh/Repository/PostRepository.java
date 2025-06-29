package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import java.util.Date;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.ApprovalStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {
        @Query("SELECT p FROM Post p WHERE p.category.categoryId = :categoryId")
        List<Post> findByCategoryId(@Param("categoryId") Integer categoryId);

        @Query("SELECT p FROM Post p JOIN FETCH p.category " +
                        "WHERE p.category.categoryId = :categoryId " +
                        "AND p.status = :status AND p.approvalStatus = :approvalStatus " +
                        "ORDER BY p.createdAt DESC")
        List<Post> findByCategoryIdAndStatusAndApprovalStatus(
                        @Param("categoryId") Integer categoryId,
                        @Param("status") Boolean status,
                        @Param("approvalStatus") ApprovalStatus approvalStatus);

        // Count posts by category
        @Query("SELECT COUNT(p) FROM Post p WHERE p.category.categoryId = :categoryId " +
                        "AND p.status = true AND p.approvalStatus = 'APPROVED'")
        Long countByCategoryId(@Param("categoryId") Integer categoryId);

        @Query("SELECT p FROM Post p WHERE p.category.categoryId = :categoryId " +
                        "AND p.status = true AND p.approvalStatus = 'APPROVED' " +
                        "ORDER BY p.price ASC")
        List<Post> findByCategorySortedByPriceAsc(@Param("categoryId") Integer categoryId);

        @Query("SELECT p FROM Post p WHERE p.category.categoryId = :categoryId " +
                        "AND p.status = true AND p.approvalStatus = 'APPROVED' " +
                        "ORDER BY p.price DESC")
        List<Post> findByCategorySortedByPriceDesc(@Param("categoryId") Integer categoryId);

        @Query("SELECT p FROM Post p WHERE p.category.categoryId = :categoryId " +
                        "AND p.status = true AND p.approvalStatus = 'APPROVED' " +
                        "ORDER BY p.createdAt DESC")
        List<Post> findByCategorySortedByCreatedAtDesc(@Param("categoryId") Integer categoryId);

        @Query("SELECT p FROM Post p WHERE p.category.categoryId = :categoryId ORDER BY p.createdAt DESC")
        List<Post> findAllByCategoryId(@Param("categoryId") Integer categoryId);

        Page<Post> findByStatusTrueAndApprovalStatusOrderByViewDesc(
                        ApprovalStatus approvalStatus, PageRequest pageRequest);

        @Query("SELECT p FROM Post p " +
                        "LEFT JOIN FETCH p.images " +
                        "LEFT JOIN FETCH p.utilities " +
                        "LEFT JOIN FETCH p.address a " +
                        "LEFT JOIN FETCH a.ward w " +
                        "LEFT JOIN FETCH w.district d " +
                        "LEFT JOIN FETCH d.province " +
                        "LEFT JOIN FETCH p.category " +
                        "LEFT JOIN FETCH p.user " +
                        "WHERE p.postId = :postId")
        Optional<Post> findByIdWithDetails(@Param("postId") Integer postId);

        @Query("SELECT DISTINCT p FROM Post p " +
                        "LEFT JOIN FETCH p.images " +
                        "LEFT JOIN FETCH p.category " +
                        "WHERE p.status = true " +
                        "ORDER BY p.createdAt DESC")
        List<Post> findAllWithDetails();

        @Query("SELECT p FROM Post p WHERE p.postId != :postId AND " +
                        "(:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
                        "AND p.status = true " +
                        "ORDER BY p.createdAt DESC")
        List<Post> findSimilarPostsByCategory(@Param("postId") Integer postId, @Param("categoryId") Integer categoryId);

        @Modifying
        @Query("UPDATE Post p SET p.view = COALESCE(p.view, 0) + 1 WHERE p.postId = :postId")
        void incrementViewCount(@Param("postId") Integer postId);

        List<Post> findByStatus(Boolean status);

        @Query("SELECT p FROM Post p LEFT JOIN FETCH p.utilities WHERE p.postId = :postId")
        Optional<Post> findByIdWithUtilities(@Param("postId") Integer postId);

        @Query("SELECT u FROM Utility u JOIN u.posts p WHERE p.postId = :postId")
        List<Utility> findUtilitiesByPostId(@Param("postId") Integer postId);

        @Query("SELECT DISTINCT p FROM Post p " +
                        "LEFT JOIN FETCH p.utilities u " +
                        "LEFT JOIN FETCH p.address a " +
                        "LEFT JOIN FETCH a.ward w " +
                        "LEFT JOIN FETCH w.district d " +
                        "LEFT JOIN FETCH d.province " +
                        "LEFT JOIN FETCH p.images i " +
                        "LEFT JOIN FETCH p.category c " +
                        "WHERE p.status = true " +
                        "AND p.approvalStatus = 'APPROVED' " +
                        "AND (:minArea IS NULL OR p.area >= :minArea) " +
                        "AND (:maxArea IS NULL OR p.area <= :maxArea) " +
                        "ORDER BY p.createdAt DESC")
        List<Post> findAllActivePostsWithAreaFilter(
                        @Param("minArea") Float minArea,
                        @Param("maxArea") Float maxArea);

        // Get posts with utility filter
        @Query("SELECT DISTINCT p FROM Post p " +
                        "LEFT JOIN FETCH p.utilities u " +
                        "LEFT JOIN FETCH p.address a " +
                        "LEFT JOIN FETCH a.ward w " +
                        "LEFT JOIN FETCH w.district d " +
                        "LEFT JOIN FETCH d.province " +
                        "LEFT JOIN FETCH p.images i " +
                        "LEFT JOIN FETCH p.category c " +
                        "WHERE p.status = true " +
                        "AND p.approvalStatus = 'APPROVED' " +
                        "AND EXISTS (SELECT 1 FROM p.utilities pu WHERE pu.utilityId IN :utilityIds) " +
                        "AND (:minArea IS NULL OR p.area >= :minArea) " +
                        "AND (:maxArea IS NULL OR p.area <= :maxArea) " +
                        "ORDER BY p.createdAt DESC")
        List<Post> findActivePostsWithUtilityFilter(
                        @Param("utilityIds") List<Integer> utilityIds,
                        @Param("minArea") Float minArea,
                        @Param("maxArea") Float maxArea);

        // Get all active posts by category (without utility filter)
        @Query("SELECT DISTINCT p FROM Post p " +
                        "LEFT JOIN FETCH p.utilities u " +
                        "LEFT JOIN FETCH p.address a " +
                        "LEFT JOIN FETCH a.ward w " +
                        "LEFT JOIN FETCH w.district d " +
                        "LEFT JOIN FETCH d.province " +
                        "LEFT JOIN FETCH p.images i " +
                        "LEFT JOIN FETCH p.category c " +
                        "WHERE p.status = true " +
                        "AND p.approvalStatus = 'APPROVED' " +
                        "AND p.category.categoryId = :categoryId " +
                        "AND (:minArea IS NULL OR p.area >= :minArea) " +
                        "AND (:maxArea IS NULL OR p.area <= :maxArea) " +
                        "ORDER BY p.createdAt DESC")
        List<Post> findActiveCategoryPostsWithAreaFilter(
                        @Param("categoryId") Integer categoryId,
                        @Param("minArea") Float minArea,
                        @Param("maxArea") Float maxArea);

        // Get posts by category with utility filter
        @Query("SELECT DISTINCT p FROM Post p " +
                        "LEFT JOIN FETCH p.utilities u " +
                        "LEFT JOIN FETCH p.address a " +
                        "LEFT JOIN FETCH a.ward w " +
                        "LEFT JOIN FETCH w.district d " +
                        "LEFT JOIN FETCH d.province " +
                        "LEFT JOIN FETCH p.images i " +
                        "LEFT JOIN FETCH p.category c " +
                        "WHERE p.status = true " +
                        "AND p.approvalStatus = 'APPROVED' " +
                        "AND p.category.categoryId = :categoryId " +
                        "AND EXISTS (SELECT 1 FROM p.utilities pu WHERE pu.utilityId IN :utilityIds) " +
                        "AND (:minArea IS NULL OR p.area >= :minArea) " +
                        "AND (:maxArea IS NULL OR p.area <= :maxArea) " +
                        "ORDER BY p.createdAt DESC")
        List<Post> findActiveCategoryPostsWithUtilityFilter(
                        @Param("categoryId") Integer categoryId,
                        @Param("utilityIds") List<Integer> utilityIds,
                        @Param("minArea") Float minArea,
                        @Param("maxArea") Float maxArea);

        @Query("SELECT p FROM Post p " +
                        "LEFT JOIN FETCH p.utilities " +
                        "LEFT JOIN FETCH p.address " +
                        "LEFT JOIN FETCH p.images " +
                        "WHERE p.status = true " +
                        "AND p.approvalStatus = :approvalStatus " +
                        "ORDER BY p.createdAt DESC")
        List<Post> findByStatusTrueAndApprovalStatusOrderByCreatedAtDesc(
                        @Param("approvalStatus") String approvalStatus);

        @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.postId = :postId")
        Optional<Post> findPostWithUserById(@Param("postId") Integer postId);

        @Query("SELECT p FROM Post p " +
                        "LEFT JOIN FETCH p.images " +
                        "LEFT JOIN FETCH p.address a " +
                        "LEFT JOIN FETCH a.ward w " +
                        "LEFT JOIN FETCH w.district d " +
                        "LEFT JOIN FETCH d.province " +
                        "WHERE p.postId = :postId")
        Optional<Post> findByIdWithShareDetails(@Param("postId") Integer postId);

        @Query("SELECT p FROM Post p " +
                        "WHERE p.category.categoryId = :categoryId " +
                        "AND (p.address.ward.district.province.id = :provinceId OR :provinceId IS NULL) " +
                        "AND p.status = true AND p.approvalStatus = 'APPROVED' " +
                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
                        "AND (:searchTerm IS NULL OR LOWER(p.address.street) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
                        +
                        "ORDER BY p.price ASC")
        List<Post> findByCategoryAndProvinceAndPriceRangeAndSearchTermSortedByPriceAsc(
                        @Param("categoryId") Integer categoryId,
                        @Param("provinceId") Integer provinceId,
                        @Param("minPrice") Float minPrice,
                        @Param("maxPrice") Float maxPrice,
                        @Param("searchTerm") String searchTerm);

        @Query("SELECT p FROM Post p " +
                        "WHERE p.category.categoryId = :categoryId " +
                        "AND (p.address.ward.district.province.id = :provinceId OR :provinceId IS NULL) " +
                        "AND p.status = true AND p.approvalStatus = 'APPROVED' " +
                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
                        "AND (:searchTerm IS NULL OR LOWER(p.address.street) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
                        +
                        "ORDER BY p.price DESC")
        List<Post> findByCategoryAndProvinceAndPriceRangeAndSearchTermSortedByPriceDesc(
                        @Param("categoryId") Integer categoryId,
                        @Param("provinceId") Integer provinceId,
                        @Param("minPrice") Float minPrice,
                        @Param("maxPrice") Float maxPrice,
                        @Param("searchTerm") String searchTerm);

        @Query("SELECT p FROM Post p " +
                        "WHERE p.category.categoryId = :categoryId " +
                        "AND (p.address.ward.district.province.id = :provinceId OR :provinceId IS NULL) " +
                        "AND p.status = true AND p.approvalStatus = 'APPROVED' " +
                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
                        "AND (:searchTerm IS NULL OR LOWER(p.address.street) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
                        +
                        "ORDER BY p.createdAt DESC")
        List<Post> findByCategoryAndProvinceAndPriceRangeAndSearchTermSortedByCreatedAtDesc(
                        @Param("categoryId") Integer categoryId,
                        @Param("provinceId") Integer provinceId,
                        @Param("minPrice") Float minPrice,
                        @Param("maxPrice") Float maxPrice,
                        @Param("searchTerm") String searchTerm);

        @Query("SELECT p FROM Post p " +
                        "WHERE p.category.categoryId = :categoryId " +
                        "AND (p.address.ward.district.province.id = :provinceId OR :provinceId IS NULL) " +
                        "AND p.status = true AND p.approvalStatus = 'APPROVED' " +
                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
                        "AND (:searchTerm IS NULL OR LOWER(p.address.street) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
        List<Post> findByCategoryAndProvinceAndPriceRangeAndSearchTerm(
                        @Param("categoryId") Integer categoryId,
                        @Param("provinceId") Integer provinceId,
                        @Param("minPrice") Float minPrice,
                        @Param("maxPrice") Float maxPrice,
                        @Param("searchTerm") String searchTerm);

        @Query("SELECT p FROM Post p WHERE p.user.userId = :userId")
        List<Post> findByUserId(@Param("userId") Integer userId);

        List<Post> findByUserOrderByCreatedAtDesc(Users user);

        @Query("SELECT p FROM Post p " +
                        "WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
                        "AND (:status IS NULL OR p.approvalStatus = :status) " +
                        "AND (:fromDate IS NULL OR p.createdAt >= :fromDate) " +
                        "AND (:toDate IS NULL OR p.createdAt <= :toDate) " +
                        "ORDER BY p.createdAt DESC")
        List<Post> searchNewestPosts(@Param("keyword") String keyword,
                        @Param("categoryId") Integer categoryId,
                        @Param("status") ApprovalStatus status,
                        @Param("fromDate") Date fromDate,
                        @Param("toDate") Date toDate);

        @Query("SELECT p FROM Post p " +
                        "WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
                        "AND (:status IS NULL OR p.approvalStatus = :status) " +
                        "AND (:fromDate IS NULL OR p.createdAt >= :fromDate) " +
                        "AND (:toDate IS NULL OR p.createdAt <= :toDate) " +
                        "ORDER BY p.createdAt ASC")
        List<Post> searchOldestPosts(@Param("keyword") String keyword,
                        @Param("categoryId") Integer categoryId,
                        @Param("status") ApprovalStatus status,
                        @Param("fromDate") Date fromDate,
                        @Param("toDate") Date toDate);

        @Query("SELECT p FROM Post p WHERE p.user = :user AND p.postId = :postId")
        Optional<Post> findByIdAndUser(@Param("postId") Integer postId, @Param("user") Users user);

        List<Post> findByUserUserIdOrderByCreatedAtDesc(Integer userId);

        List<Post> findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus status);

        List<Post> findByApprovalStatusAndStatusOrderByCreatedAtDesc(ApprovalStatus approvalStatus, Boolean status);

        List<Post> findByStatusTrueAndApprovalStatusOrderByCreatedAtDesc(ApprovalStatus approvalStatus);

}
