package nhatroxanh.com.Nhatroxanh.Service.Impl;

import java.util.List;
import java.util.Set;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.sql.*;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import nhatroxanh.com.Nhatroxanh.Model.enity.Address;
import nhatroxanh.com.Nhatroxanh.Model.enity.ApprovalStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Category;
import nhatroxanh.com.Nhatroxanh.Model.enity.Image;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;
import nhatroxanh.com.Nhatroxanh.Model.enity.Ward;
import nhatroxanh.com.Nhatroxanh.Repository.AddressRepository;
import nhatroxanh.com.Nhatroxanh.Repository.CategoryRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ImageRepository;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UtilityRepository;
import nhatroxanh.com.Nhatroxanh.Repository.WardRepository;
import nhatroxanh.com.Nhatroxanh.Service.FileUploadService;
import nhatroxanh.com.Nhatroxanh.Service.PostService;

@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UtilityRepository utilityRepository;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private WardRepository wardRepository;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private FileUploadService fileUploadService;

    @Override
    public List<Post> findTopApprovedActivePostsByViews(int limit) {
        return postRepository.findByStatusTrueAndApprovalStatusOrderByViewDesc(
                ApprovalStatus.APPROVED, PageRequest.of(0, limit)).getContent();
    }

    @Override
    public List<Post> filterPosts(List<Integer> utilityIds, Float minArea, Float maxArea, String sort) {
        List<Post> posts;
        if (utilityIds != null && !utilityIds.isEmpty()) {
            posts = postRepository.findActivePostsWithUtilityFilter(utilityIds, minArea, maxArea);
        } else {
            posts = postRepository.findAllActivePostsWithAreaFilter(minArea, maxArea);
        }

        applySorting(posts, sort);
        return posts;
    }

    @Override
    public List<Post> filterPostsByCategory(Integer categoryId, List<Integer> utilityIds, Float minArea, Float maxArea,
            String sort) {
        List<Post> posts;
        if (utilityIds != null && !utilityIds.isEmpty()) {
            posts = postRepository.findActiveCategoryPostsWithUtilityFilter(categoryId, utilityIds, minArea, maxArea);
        } else {
            posts = postRepository.findActiveCategoryPostsWithAreaFilter(categoryId, minArea, maxArea);
        }

        applySorting(posts, sort);
        return posts;
    }

    private void applySorting(List<Post> posts, String sort) {
        if (sort != null) {
            switch (sort) {
                case "price_asc":
                    posts.sort((a, b) -> Float.compare(a.getPrice(), b.getPrice()));
                    break;
                case "price_desc":
                    posts.sort((a, b) -> Float.compare(b.getPrice(), a.getPrice()));
                    break;
                case "latest":
                    posts.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public List<Post> getAllActivePosts() {
        return postRepository.findByStatusTrueAndApprovalStatusOrderByCreatedAtDesc("APPROVED");
    }

    @Override
    public List<Post> getPostsByUserId(Integer userId) {
        return postRepository.findByUserId(userId);
    }

    @Override
    public List<Post> searchPosts(String keyword, Integer categoryId, ApprovalStatus status,
            Date fromDate, Date toDate, String sort) {
        if (sort == null || sort.isEmpty() || sort.equals("newest")) {
            return postRepository.searchNewestPosts(keyword, categoryId, status, fromDate, toDate);
        } else {
            return postRepository.searchOldestPosts(keyword, categoryId, status, fromDate, toDate);
        }
    }

    @Override
    public Post getPostById(Integer postId) {
        return postRepository.findById(postId).orElse(null);
    }

    @Override
    public void deletePost(Integer postId) {
        postRepository.deleteById(postId);
    }

    @Override
    public void savePost(Post post) {
        postRepository.save(post);
    }

    public Post createPost(String title, String description, Float price, Float area,
            Integer categoryId, Integer wardId, String street, String houseNumber,
            List<Integer> utilityIds, MultipartFile[] images, Users user) throws Exception {

        validatePostData(title, description, price, area);
        Address address = createAddress(wardId, street, houseNumber);

        Category category = null;
        if (categoryId != null && categoryId > 0) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại với ID: " + categoryId));
        }

        Set<Utility> utilities = new HashSet<>();
        if (utilityIds != null && !utilityIds.isEmpty()) {
            // Filter out invalid or negative IDs
            List<Integer> validUtilityIds = utilityIds.stream()
                    .filter(id -> id != null && id > 0)
                    .toList();
            utilities = new HashSet<>(utilityRepository.findAllById(validUtilityIds));
            if (validUtilityIds.size() != utilities.size()) {
                System.out.println("Một số tiện ích không tồn tại: " + validUtilityIds);
            }
        }

        Post post = Post.builder()
                .title(title.trim())
                .description(description != null ? description.trim() : "")
                .price(price)
                .area(area)
                .view(0)
                .status(true)
                .approvalStatus(ApprovalStatus.PENDING)
                .createdAt(java.sql.Date.valueOf(LocalDate.now()))
                .user(user)
                .address(address)
                .category(category)
                .utilities(utilities)
                .build();

        post = postRepository.save(post);

        if (images != null && images.length > 0) {
            List<Image> imageList = uploadPostImages(images, post);
            post.setImages(imageList);
        }

        return postRepository.save(post);
    }

    private void validatePostData(String title, String description, Float price, Float area) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu đề không được để trống");
        }
        if (title.trim().length() > 255) {
            throw new IllegalArgumentException("Tiêu đề không được vượt quá 255 ký tự");
        }
        if (price == null || price <= 0) {
            throw new IllegalArgumentException("Giá thuê phải lớn hơn 0");
        }
        if (price > 1_000_000_000) {
            throw new IllegalArgumentException("Giá thuê không được vượt quá 1 tỷ VNĐ");
        }
        if (area == null || area <= 0) {
            throw new IllegalArgumentException("Diện tích phải lớn hơn 0");
        }
        if (area > 10_000) {
            throw new IllegalArgumentException("Diện tích không được vượt quá 10,000 m²");
        }
        if (description != null && description.length() > 5000) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 5,000 ký tự");
        }
    }

    private Address createAddress(Integer wardId, String street, String houseNumber) {
        Ward ward = null;
        if (wardId != null && wardId > 0) {
            ward = wardRepository.findById(wardId).orElse(null);
        }

        String fullStreet = "";
        if (houseNumber != null && !houseNumber.trim().isEmpty()) {
            fullStreet += houseNumber.trim();
        }
        if (street != null && !street.trim().isEmpty()) {
            if (!fullStreet.isEmpty())
                fullStreet += ", ";
            fullStreet += street.trim();
        }

        Address address = Address.builder()
                .street(fullStreet)
                .ward(ward)
                .build();

        return addressRepository.save(address);
    }

    private List<Image> uploadPostImages(MultipartFile[] images, Post post) throws Exception {
        List<Image> imageList = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (images.length > 8) {
            throw new IllegalArgumentException("Không được tải lên quá 8 ảnh");
        }

        for (int i = 0; i < images.length; i++) {
            MultipartFile imageFile = images[i];
            if (!imageFile.isEmpty()) {
                try {
                    if (imageFile.getSize() > 10 * 1024 * 1024) {
                        errors.add("Ảnh " + (i + 1) + " quá lớn (tối đa 10MB)");
                        continue;
                    }

                    String imageUrl = fileUploadService.uploadFile(imageFile, "");
                    Image image = Image.builder().url(imageUrl).post(post).build();
                    imageList.add(imageRepository.save(image));
                } catch (Exception e) {
                    errors.add("Lỗi khi upload ảnh " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        if (!errors.isEmpty() && imageList.isEmpty()) {
            throw new Exception("Không thể upload ảnh nào: " + String.join(", ", errors));
        }

        if (!errors.isEmpty()) {
            System.out.println("Một số ảnh không thể upload: " + String.join(", ", errors));
        }

        return imageList;
    }
}
