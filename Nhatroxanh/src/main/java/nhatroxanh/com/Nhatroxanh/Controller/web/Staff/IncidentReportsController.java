package nhatroxanh.com.Nhatroxanh.Controller.web.Staff;

import java.sql.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.entity.IncidentReports;
import nhatroxanh.com.Nhatroxanh.Repository.IncidentReportsRepository;

@Controller
@RequestMapping("/nhan-vien")
public class IncidentReportsController {

    @Autowired
    private IncidentReportsRepository incidentReportsRepository;

    @GetMapping("/khieu-nai")
    public String viewReports(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<IncidentReports> reportPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            reportPage = incidentReportsRepository.searchByKeyword(keyword.trim(), pageable);
        } else {
            reportPage = incidentReportsRepository.findAllPaged(pageable);
        }

        model.addAttribute("reportPage", reportPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reportPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "staff/khieu-nai";
    }

    @GetMapping("/chi-tiet-khieu-nai/{id}")
    public String viewComplaintDetail(@PathVariable("id") Integer id, Model model) {
        IncidentReports report = incidentReportsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khiếu nại với ID: " + id));

        model.addAttribute("report", report);
        return "staff/chitiet-khieunai";
    }

    @PostMapping("/cap-nhat-trang-thai")
    public String updateReportStatus(
            @RequestParam("reportId") Integer reportId,
            @RequestParam("status") String newStatus,
            RedirectAttributes redirectAttributes) {

        Optional<IncidentReports> optional = incidentReportsRepository.findById(reportId);

        if (optional.isPresent()) {
            IncidentReports report = optional.get();

            IncidentReports.IncidentStatus statusEnum = IncidentReports.IncidentStatus.valueOf(newStatus);
            report.setStatus(statusEnum);

            if (statusEnum == IncidentReports.IncidentStatus.DANG_XU_LY && report.getResolvedAt() == null) {
                report.setResolvedAt(new java.sql.Date(System.currentTimeMillis()));
            }

            incidentReportsRepository.save(report);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy báo cáo!");
        }

        return "redirect:/nhan-vien/chi-tiet-khieu-nai/" + reportId;
    }

}
