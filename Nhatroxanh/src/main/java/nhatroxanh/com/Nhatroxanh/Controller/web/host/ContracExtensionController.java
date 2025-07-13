package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.ExtensionRequests;
import nhatroxanh.com.Nhatroxanh.Model.enity.ExtensionRequests.RequestStatus;
import nhatroxanh.com.Nhatroxanh.Repository.ContractsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ExtensionRequestRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;

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
    public String listExtensionRequests(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Integer ownerId = userDetails.getUser().getUserId();
        List<ExtensionRequests> requests = extensionRequestRepository.findAllByOwnerId(ownerId);
        model.addAttribute("requests", requests);
        return "host/gia-han-tra-phong-host";
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

}
