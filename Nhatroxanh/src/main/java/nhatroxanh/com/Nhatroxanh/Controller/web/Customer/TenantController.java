package nhatroxanh.com.Nhatroxanh.Controller.web.Customer;

import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Image;
import nhatroxanh.com.Nhatroxanh.Model.enity.IncidentReports;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Review;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;
import nhatroxanh.com.Nhatroxanh.Repository.ContractRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ImageRepository;
import nhatroxanh.com.Nhatroxanh.Repository.IncidentReportsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ReviewRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.ReviewService;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;
import nhatroxanh.com.Nhatroxanh.Service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Pageable;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/khach-thue")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private IncidentReportsRepository incidentReportsRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private RoomsService roomService;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @GetMapping("/quan-ly-thue-tra")
    public String showRentalManagementPage(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("endDate").descending());
        Page<Contracts> contractHistoryPage = tenantService.getContractHistory(pageable);

        List<Contracts> activeContracts = tenantService.getActiveContracts();
        Map<String, Object> quickStats = tenantService.getQuickStats(pageable);

        model.addAttribute("activeContracts", activeContracts);
        model.addAttribute("contractHistory", contractHistoryPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", contractHistoryPage.getTotalPages());
        model.addAttribute("totalItems", contractHistoryPage.getTotalElements());
        model.addAttribute("quickStats", quickStats);

        return "guest/quan-ly-thue-tra";
    }

    @GetMapping("/chitiet-phongthue/{id}")
    public String showContractDetails(@PathVariable("id") Integer contractId,
            @RequestParam(value = "editId", required = false) Integer editId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        Contracts contract = tenantService.getContractById(contractId);
        if (contract == null || !contract.getTenant().getUserId().equals(userDetails.getUserId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Bạn không có quyền truy cập hợp đồng này.");
            return "redirect:/khach-thue/quan-ly-thue-tra";
        }

        List<IncidentReports> incidentReports = tenantService.getIncidentReportsByRoom(contract.getRoom().getRoomId());
        Set<Utility> utilities = tenantService.getUtilitiesByRoomId(contract.getRoom().getRoomId());

        model.addAttribute("contract", contract);
        model.addAttribute("incidentReports", incidentReports);
        model.addAttribute("utilities", utilities != null ? utilities : new HashSet<>());

        // Nếu có editId thì thêm báo cáo cần sửa
        if (editId != null) {
            IncidentReports editReport = incidentReportsRepository.findById(editId).orElse(null);
            model.addAttribute("editReport", editReport);
            model.addAttribute("showEditModal", true);
        }

        return "guest/chitiet-phongthue";
    }

    @PostMapping("/tra-phong")
    public String returnRoom(@RequestParam("contractId") Integer contractId,
            @RequestParam("returnDate") Date returnDate,
            @RequestParam(value = "returnReason", required = false) String reason) {
        tenantService.returnRoom(contractId, returnDate, reason);
        return "redirect:/khach-thue/quan-ly-thue-tra";
    }

    @PostMapping("/danh-gia")
    public String submitReview(@RequestParam("roomId") Integer roomId,
            @RequestParam("rating") Double rating,
            @RequestParam(value = "comment", required = false) String comment) {
        tenantService.submitReview(roomId, rating, comment);
        return "redirect:/khach-thue/quan-ly-thue-tra";
    }

    @PostMapping("/bao-cao-su-co")
    public String submitIncidentReport(@RequestParam("roomId") Integer roomId,
            @RequestParam("incidentType") String incidentType,
            @RequestParam("description") String description,
            @RequestParam("level") String level) {
        tenantService.submitIncidentReport(roomId, incidentType, description,
                IncidentReports.IncidentLevel.valueOf(level.toUpperCase()));
        return "redirect:/khach-thue/chitiet-phongthue/" + roomId;
    }

    @PostMapping("/gia-han")
    public String extendContract(@RequestParam("contractId") Integer contractId,
            @RequestParam("newEndDate") Date newEndDate,
            @RequestParam(value = "message", required = false) String message) {
        tenantService.extendContract(contractId, newEndDate, message);
        return "redirect:/khach-thue/chitiet-phongthue/" + contractId;
    }

    @GetMapping("/chi-tiet-hop-dong/{id}/json")
    @ResponseBody
    public Contracts viewContractDetails(@PathVariable("id") Integer contractId) {
        return tenantService.getContractById(contractId);
    }

    @PostMapping("/bao-cao")
    public String submitIncidentReport(
            @RequestParam("contractId") Integer contractId,
            @RequestParam("roomId") Integer roomId,
            @RequestParam("incidentType") String incidentType,
            @RequestParam("description") String description,
            @RequestParam("level") String level,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        System.out.println("🔍 Controller received images: " + (images != null ? images.size() : "null"));
        try {
            tenantService.createIncidentReport(roomId, incidentType, description, level, images, userDetails);
            redirectAttributes.addFlashAttribute("successMessage", "Báo cáo đã được gửi thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gửi báo cáo thất bại: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/khach-thue/chitiet-phongthue/" + contractId;
    }

    @PostMapping("/xoa-bao-cao/{id}")
    public String deleteReport(@RequestParam("contractId") Integer contractId,
            @PathVariable("id") Integer reportId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            IncidentReports report = incidentReportsRepository.findById(reportId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy báo cáo"));

            incidentReportsRepository.delete(report);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa báo cáo thành công.");
            return "redirect:/khach-thue/chitiet-phongthue/" + contractId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa báo cáo thất bại: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/khach-thue/chitiet-phongthue/" + contractId;
        }
    }

    @PostMapping("/cap-nhat-bao-cao")
    public String updateIncidentReport(
            @RequestParam("reportId") Integer reportId,
            @RequestParam("contractId") Integer contractId,
            @RequestParam("roomId") Integer roomId,
            @RequestParam("incidentType") String incidentType,
            @RequestParam("description") String description,
            @RequestParam("level") String level,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "imageIdsToDelete", required = false) List<Integer> imageIdsToDelete,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            tenantService.updateIncidentReport(reportId, roomId, incidentType, description, level, images,
                    imageIdsToDelete, userDetails);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật báo cáo thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật báo cáo thất bại: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/khach-thue/chitiet-phongthue/" + contractId;
    }

    @PostMapping("/xoa-anh")
    @ResponseBody
    public ResponseEntity<?> deleteReportImageAjax(
            @RequestParam("imageId") Integer imageId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Image image = imageRepository.findById(imageId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh"));

            if (!image.getReport().getUser().getUserId().equals(userDetails.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Không có quyền xóa ảnh này.");
            }

            // Xóa ảnh khỏi DB và (nếu muốn) file server
            imageRepository.delete(image);
            // fileUploadService.deleteFile(image.getUrl());

            return ResponseEntity.ok("Đã xóa ảnh thành công.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xóa ảnh: " + e.getMessage());
        }
    }

    @GetMapping("/tra-phong/{id}")
    public String showReturnRoomForm(@PathVariable("id") Integer contractId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        Contracts contract = tenantService.getContractById(contractId);
        if (contract == null || !contract.getTenant().getUserId().equals(userDetails.getUserId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Bạn không có quyền truy cập hợp đồng này.");
            return "redirect:/khach-thue/quan-ly-thue-tra";
        }
        model.addAttribute("contract", contract);
        return "guest/tra-phong";
    }

    @PostMapping("/tra-phong/{id}")
    public String returnRoom(@PathVariable("id") Integer contractId,
            @RequestParam("returnDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate returnDate,
            @RequestParam(value = "returnReason", required = false) String returnReason,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            if (returnDate.isBefore(LocalDate.now())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Ngày trả phòng không thể là ngày trong quá khứ.");
                return "redirect:/khach-thue/tra-phong/" + contractId;
            }

            tenantService.returnRoom(contractId, Date.valueOf(returnDate), returnReason);
            redirectAttributes.addFlashAttribute("successMessage", "Gửi yêu cầu trả phòng thành công.");
            return "redirect:/khach-thue/quan-ly-thue-tra";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gửi yêu cầu trả phòng thất bại: " + e.getMessage());
            return "redirect:/khach-thue/tra-phong/" + contractId;
        }
    }

    @GetMapping("/gia-han/{id}")
    public String showExtendForm(@PathVariable("id") Integer contractId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Contracts contract = tenantService.getContractById(contractId);
            if (contract == null || contract.getTenant() == null
                    || !contract.getTenant().getUserId().equals(userDetails.getUserId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập hợp đồng này.");
                return "redirect:/khach-thue/quan-ly-thue-tra";
            }

            model.addAttribute("contract", contract);
            return "guest/gia-han";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/khach-thue/quan-ly-thue-tra";
        }
    }

    @PostMapping("/gia-han/{id}")
    public String requestExtension(@PathVariable("id") Integer contractId,
            @RequestParam("requestedExtendDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate requestedExtendDate,
            @RequestParam(value = "message", required = false) String message,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            tenantService.createExtensionRequest(contractId, requestedExtendDate, message, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu gia hạn đã được gửi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gửi yêu cầu thất bại: " + e.getMessage());
        }

        return "redirect:/khach-thue/chitiet-phongthue/" + contractId;
    }

    @GetMapping("/danh-gia")
    public String showRatingForm(@RequestParam("contractId") Integer contractId,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/dang-nhap";
        }

        Users user = userDetails.getUser();
        Contracts contract = contractRepository.findById(contractId).orElse(null);

        if (contract == null || contract.getTenant() == null ||
                !contract.getTenant().getUserId().equals(user.getUserId()) ||
                contract.getStatus() != Contracts.Status.ACTIVE) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không thể đánh giá phòng này.");
            return "redirect:/khach-thue/chitiet-phongthue/" + contractId;
        }

        if (reviewRepository.existsByContract(contract)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã đánh giá phòng này rồi.");
            return "redirect:/khach-thue/chitiet-phongthue/" + contractId;
        }

        model.addAttribute("review", new Review());
        model.addAttribute("contractId", contractId);
        model.addAttribute("contract", contract);
        return "guest/danh-gia";
    }

    @PostMapping("/danh-gia-phong")
    public String submitRating(@Valid @ModelAttribute("review") Review review,
            BindingResult result,
            @RequestParam("contractId") Integer contractId,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/dang-nhap";
        }

        Users user = userDetails.getUser();
        Contracts contract = contractRepository.findById(contractId).orElse(null);

        if (contract == null || contract.getTenant() == null ||
                !contract.getTenant().getUserId().equals(user.getUserId()) ||
                contract.getStatus() != Contracts.Status.ACTIVE) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không thể đánh giá phòng này.");
            return "redirect:/khach-thue/chitiet-phongthue/" + contractId;
        }

        if (result.hasErrors()) {
            model.addAttribute("contractId", contractId);
            model.addAttribute("contract", contract);
            return "guest/danh-gia";
        }

        if (reviewRepository.existsByContract(contract)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã đánh giá phòng này rồi.");
            return "redirect:/khach-thue/chitiet-phongthue/" + contractId;
        }

        review.setUser(user);
        review.setRoom(contract.getRoom());
        review.setContract(contract);
        review.setPost(null);
        review.setCreatedAt(new Date(System.currentTimeMillis()));

        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute("successMessage", "Đánh giá của bạn đã được gửi thành công.");
        return "redirect:/khach-thue/chitiet-phongthue/" + contractId;
    }
}
