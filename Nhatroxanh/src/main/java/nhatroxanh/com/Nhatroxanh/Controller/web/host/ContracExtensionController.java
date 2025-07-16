package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.ExtensionRequests;
import nhatroxanh.com.Nhatroxanh.Model.enity.ExtensionRequests.RequestStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.RoomStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.ContractsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ExtensionRequestRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Date;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/chu-tro/gia-hang-tra-phong")
public class ContracExtensionController {

    private final ExtensionRequestRepository extensionRequestRepository;
    private final ContractsRepository contractsRepository;
    private final EmailService emailService;

    @GetMapping
    public String listRequestsByTab(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "extension") String tab,
            Model model) {
        try {
            if (page < 0)
                page = 0;
            if (size <= 0)
                size = 6;

            Integer ownerId = userDetails.getUser().getUserId();
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

            if (tab.equals("return")) {
                Page<Contracts> returnPage = keyword.trim().isEmpty()
                        ? contractsRepository.findReturnRequestsByOwner(ownerId, pageable)
                        : contractsRepository.findReturnRequestsByOwnerAndKeyword(ownerId, keyword.trim(), pageable);

                model.addAttribute("returnRequests", returnPage.getContent());
                model.addAttribute("totalItems", returnPage.getTotalElements());
                model.addAttribute("totalPages", returnPage.getTotalPages());
            } else {
                Page<ExtensionRequests> extensionPage = keyword.trim().isEmpty()
                        ? extensionRequestRepository.findAllByOwner(ownerId, pageable)
                        : extensionRequestRepository.findByOwnerIdAndKeyword(ownerId, keyword.trim(), pageable);

                model.addAttribute("requests", extensionPage.getContent());
                model.addAttribute("totalItems", extensionPage.getTotalElements());
                model.addAttribute("totalPages", extensionPage.getTotalPages());
            }

            model.addAttribute("currentPage", page);
            model.addAttribute("size", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("tab", tab);
            model.addAttribute("RequestStatus", ExtensionRequests.RequestStatus.class);
            model.addAttribute("ReturnStatus", Contracts.ReturnStatus.class);

            return "host/gia-han-tra-phong-host";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách yêu cầu.");
            return "host/gia-han-tra-phong-host";
        }
    }

    @PostMapping("/duyet")
    @Transactional
    public String approveExtension(@RequestParam("requestId") Integer requestId,
            RedirectAttributes redirectAttributes) {
        try {
            ExtensionRequests req = extensionRequestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("Yêu cầu không tồn tại"));
            if (req.getStatus() == RequestStatus.PENDING) {
                req.setStatus(RequestStatus.APPROVED);

                Contracts contract = req.getContract();
                contract.setEndDate(req.getRequestedExtendDate());
                contractsRepository.save(contract);
                extensionRequestRepository.save(req);

                // Gửi email
                String email = req.getContract().getTenant().getEmail();
                String fullname = req.getContract().getTenant().getFullname();
                String contractCode = "#CTR" + contract.getContractId();
                Date newEndDate = req.getRequestedExtendDate();

                emailService.sendExtensionApprovalEmail(email, fullname, contractCode, newEndDate);

                redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt yêu cầu thành công.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Yêu cầu không hợp lệ.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi phê duyệt.");
        }
        return "redirect:/chu-tro/gia-hang-tra-phong";
    }

    @PostMapping("/tu-choi")
    @Transactional
    public String rejectExtension(@RequestParam("requestId") Integer requestId, RedirectAttributes redirectAttributes) {
        try {
            ExtensionRequests req = extensionRequestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("Yêu cầu không tồn tại"));
            if (req.getStatus() == RequestStatus.PENDING) {
                req.setStatus(RequestStatus.REJECTED);
                extensionRequestRepository.save(req);

                String email = req.getContract().getTenant().getEmail();
                String fullname = req.getContract().getTenant().getFullname();
                String contractCode = "#CTR" + req.getContract().getContractId();
                String reason = "Không phù hợp với chính sách hoặc thời gian không khả dụng.";

                emailService.sendExtensionRejectionEmail(email, fullname, contractCode, reason);

                redirectAttributes.addFlashAttribute("successMessage", "Từ chối yêu cầu thành công.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Yêu cầu không hợp lệ.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi từ chối.");
        }
        return "redirect:/chu-tro/gia-hang-tra-phong";
    }

    @PostMapping("/duyet-tra-phong/{id}")
    public String approveReturnRoom(@PathVariable("id") Integer contractId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Contracts contract = contractsRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Hợp đồng không tồn tại"));

            if (!contract.getOwner().getUserId().equals(userDetails.getUser().getUserId())) {
                throw new RuntimeException("Không có quyền duyệt hợp đồng này.");
            }

            contract.setStatus(Contracts.Status.TERMINATED);
            contract.setReturnStatus(Contracts.ReturnStatus.APPROVED);

            if (contract.getRoom() != null) {
                contract.getRoom().setStatus(RoomStatus.unactive);
            }

            contractsRepository.save(contract);

            // Gửi email thông báo duyệt
            Users tenant = contract.getTenant();
            if (tenant != null) {
                String to = tenant.getEmail();
                String fullname = tenant.getFullname();
                String contractCode = "#CTR" + contract.getContractId();
                Date endDate = contract.getEndDate();
                emailService.sendReturnApprovalEmail(to, fullname, contractCode, endDate);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt trả phòng và gửi email thông báo.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi duyệt trả phòng: " + e.getMessage());
        }

        return "redirect:/chu-tro/gia-hang-tra-phong?tab=return";
    }

    @PostMapping("/tu-choi-tra-phong/{id}")
    public String rejectReturnRoom(@PathVariable("id") Integer contractId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Contracts contract = contractsRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Hợp đồng không tồn tại"));

            if (!contract.getOwner().getUserId().equals(userDetails.getUser().getUserId())) {
                throw new RuntimeException("Không có quyền từ chối hợp đồng này.");
            }

            contract.setReturnStatus(Contracts.ReturnStatus.REJECTED);
            contractsRepository.save(contract);

            // Gửi email thông báo từ chối
            Users tenant = contract.getTenant();
            if (tenant != null) {
                String to = tenant.getEmail();
                String fullname = tenant.getFullname();
                String contractCode = "#CTR" + contract.getContractId();
                String reason = "Yêu cầu không hợp lệ hoặc chưa đủ điều kiện trả phòng.";
                emailService.sendReturnRejectionEmail(to, fullname, contractCode, reason);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối trả phòng và gửi email thông báo.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi từ chối trả phòng: " + e.getMessage());
        }

        return "redirect:/chu-tro/gia-hang-tra-phong?tab=return";
    }

}
