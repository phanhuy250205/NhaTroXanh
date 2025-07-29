package nhatroxanh.com.Nhatroxanh.Service.Impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import nhatroxanh.com.Nhatroxanh.Model.entity.Address;
import nhatroxanh.com.Nhatroxanh.Model.entity.ApprovalStatus;
import nhatroxanh.com.Nhatroxanh.Model.entity.Category;
import nhatroxanh.com.Nhatroxanh.Model.entity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.entity.Image;
import nhatroxanh.com.Nhatroxanh.Model.entity.Post;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Model.entity.Utility;
import nhatroxanh.com.Nhatroxanh.Repository.CategoryRepository;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ImageRepository;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UtilityRepository;
import nhatroxanh.com.Nhatroxanh.Service.AddressService;
import nhatroxanh.com.Nhatroxanh.Service.FileUploadService;
import nhatroxanh.com.Nhatroxanh.Service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UtilityRepository utilityRepository;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private FileUploadService fileUploadService;
    @Autowired
    private HostelRepository hostelRepository;
    @Autowired
    private AddressService addressService;

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
    public Page<Post> getPostsByUserId(Integer userId, Pageable pageable) {
        return postRepository.findByUserUserId(userId, pageable);
    }

    @Override
    public Page<Post> searchPosts(String keyword, Integer categoryId, ApprovalStatus status,
            Date fromDate, Date toDate, String sort, Integer userId, Pageable pageable) {
        if (sort == null || sort.isEmpty() || sort.equals("newest")) {
            return postRepository.searchNewestPostsPaged(keyword, categoryId, status, fromDate, toDate, userId,
                    pageable);
        } else {
            return postRepository.searchOldestPostsPaged(keyword, categoryId, status, fromDate, toDate, userId,
                    pageable);
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

    @Transactional
    public Post createPost(String title, String description, Float price, Float area,
            Integer categoryId, String wardCode, String street, String houseNumber,
            List<Integer> utilityIds, MultipartFile[] images, Integer hostelId, Users user,
            String provinceCode, String districtCode, String provinceName,
            String districtName, String wardName) throws Exception {

        // Kiểm tra dữ liệu đầu vào
        validatePostData(title, description, price, area);

        // Xử lý địa chỉ từ API
        String fullStreet = houseNumber != null && !houseNumber.trim().isEmpty()
                ? houseNumber.trim() + ", " + street.trim()
                : street.trim();
        Address address = addressService.processAddressFromApi(
                provinceCode, districtCode, wardCode,
                provinceName, districtName, wardName, fullStreet);

        // Lấy danh mục
        Category category = null;
        if (categoryId != null && categoryId > 0) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại với ID: " + categoryId));
        }

        // Lấy và kiểm tra nhà trọ
        Hostel hostel = null;
        if (hostelId != null && hostelId > 0) {
            hostel = hostelRepository.findById(hostelId)
                    .orElseThrow(() -> new IllegalArgumentException("Nhà trọ không tồn tại với ID: " + hostelId));
            // Kiểm tra quyền sở hữu
            if (!hostel.getOwner().getUserId().equals(user.getUserId())) {
                throw new IllegalArgumentException("Bạn không có quyền đăng bài cho nhà trọ này");
            }
        }

        // Lấy tiện ích
        Set<Utility> utilities = new HashSet<>();
        if (utilityIds != null && !utilityIds.isEmpty()) {
            List<Integer> validUtilityIds = utilityIds.stream()
                    .filter(id -> id != null && id > 0)
                    .toList();
            utilities = new HashSet<>(utilityRepository.findAllById(validUtilityIds));
            if (validUtilityIds.size() != utilities.size()) {
                System.out.println("Một số tiện ích không tồn tại: " + validUtilityIds);
            }
        }

        // Tạo bài đăng
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
                .hostel(hostel)
                .build();

        // Lưu bài đăng
        post = postRepository.save(post);

        // Xử lý hình ảnh
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
                    Image image = Image.builder()
                            .url(imageUrl)
                            .post(post)
                            .build();
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

    @Transactional
    public Post updatePost(Integer postId, String title, String description, Float price, Float area,
            Integer categoryId, String wardCode, String street, String houseNumber,
            List<Integer> utilityIds, MultipartFile[] images, List<Integer> imagesToDelete,
            List<Integer> imagesToKeep, Integer hostelId, Users user, String provinceCode, String districtCode,
            String provinceName, String districtName, String wardName) throws Exception {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Bài đăng không tồn tại với ID: " + postId));

        if (!post.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa bài đăng này");
        }

        validatePostData(title, description, price, area);

        String fullStreet = (houseNumber != null && !houseNumber.trim().isEmpty() ? houseNumber.trim() + ", " : "")
                + (street != null ? street.trim() : "");
        Address address = addressService.processAddressFromApi(provinceCode, districtCode, wardCode,
                provinceName, districtName, wardName, fullStreet);

        Category category = categoryId != null && categoryId > 0 ? categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại với ID: " + categoryId)) : null;

        Hostel hostel = hostelId != null && hostelId > 0 ? hostelRepository.findById(hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Nhà trọ không tồn tại với ID: " + hostelId)) : null;
        if (hostel != null && !hostel.getOwner().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("Bạn không có quyền đăng bài cho nhà trọ này");
        }

        Set<Utility> utilities = new HashSet<>();
        if (utilityIds != null && !utilityIds.isEmpty()) {
            List<Integer> validUtilityIds = utilityIds.stream().filter(id -> id != null && id > 0).toList();
            utilities = new HashSet<>(utilityRepository.findAllById(validUtilityIds));
        }

        post.setTitle(title.trim());
        post.setDescription(description != null ? description.trim() : "");
        post.setPrice(price);
        post.setArea(area);
        post.setAddress(address);
        post.setCategory(category);
        post.setUtilities(utilities);
        post.setHostel(hostel);
        post.setCreatedAt(java.sql.Date.valueOf(LocalDate.now()));

        if (imagesToDelete != null && !imagesToDelete.isEmpty()) {
            List<Image> imagesToRemove = imageRepository.findAllById(imagesToDelete);
            log.info("Images to delete: {}", imagesToRemove);

            for (Image image : imagesToRemove) {
                if (image.getPost() != null && image.getPost().getPostId().equals(postId)) {
                    post.getImages().remove(image);
                    image.setPost(null);
                    imageRepository.save(image);
                    fileUploadService.deleteFile(image.getUrl());
                    imageRepository.delete(image);
                }
            }
            entityManager.flush();
        }
        if (imagesToKeep != null && !imagesToKeep.isEmpty()) {
            List<Image> imagesKept = imageRepository.findAllById(imagesToKeep);
            post.setImages(imagesKept); // Cập nhật danh sách ảnh còn lại
        }

        // Thêm ảnh mới
        if (images != null && images.length > 0) {
            List<Image> newImages = uploadPostImages(images, post);
            post.getImages().addAll(newImages);
        }

        return postRepository.save(post);
    }

    @Override
    public void save(Post post) {
        postRepository.save(post);
    }

    @Override
    public List<Post> getFilteredPosts(String status, String type, String sortBy, String search) {
        List<Post> posts = postRepository.findAll();

        if (status != null && !status.isEmpty()) {
            ApprovalStatus approvalStatus = ApprovalStatus.valueOf(status.toUpperCase());
            posts = posts.stream()
                    .filter(post -> post.getApprovalStatus() == approvalStatus)
                    .collect(Collectors.toList());
        }

        if (type != null && !type.isEmpty()) {
            posts = posts.stream()
                    .filter(post -> post.getCategory() != null &&
                            post.getCategory().getName() != null &&
                            post.getCategory().getName().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        if (search != null && !search.isEmpty()) {
            posts = posts.stream()
                    .filter(post -> post.getTitle().toLowerCase().contains(search.toLowerCase()) ||
                            post.getDescription().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if ("newest".equalsIgnoreCase(sortBy)) {
            posts.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));
        } else if ("oldest".equalsIgnoreCase(sortBy)) {
            posts.sort((p1, p2) -> p1.getCreatedAt().compareTo(p2.getCreatedAt()));
        }

        return posts;
    }

    @Override
    public void approvePost(Integer postId, Users approvedBy) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài đăng với ID: " + postId));

        post.setApprovalStatus(ApprovalStatus.APPROVED);
        post.setApprovedBy(approvedBy);
        post.setApprovedAt(java.sql.Date.valueOf(LocalDate.now()));
        post.setStatus(true);
        postRepository.save(post);
    }

    @Override
    public void hidePost(Integer postId) {
        Post post = getPostById(postId);
        post.setStatus(false);
        postRepository.save(post);
    }

    @Override
    public Page<Post> getFilteredPostsByApprovalStatus(
            ApprovalStatus approvalStatus,
            String type,
            String search,
            Pageable pageable) {

        if (type != null && type.trim().isEmpty()) {
            type = null;
        }
        if (search != null && search.trim().isEmpty()) {
            search = null;
        }

        return postRepository.findFilteredPosts(approvalStatus, type, search, pageable);
    }

}
