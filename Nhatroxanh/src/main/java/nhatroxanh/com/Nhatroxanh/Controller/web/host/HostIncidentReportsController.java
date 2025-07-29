package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.entity.IncidentReports;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;
import nhatroxanh.com.Nhatroxanh.Service.IncidentReportsService;

@Controller
@RequestMapping("/chu-tro/quan-ly-su-co")
public class HostIncidentReportsController {
    @Autowired
    private IncidentReportsService incidentService;

    @Autowired
    private EmailService emailService;

    @GetMapping
    public String listIncidents(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model) {

        Integer hostId = userDetails.getUserId();
        Page<IncidentReports> incidentsPage = incidentService.getIncidentsByHostId(hostId, search, page, size);

        model.addAttribute("incidents", incidentsPage.getContent());
        model.addAttribute("currentPage", incidentsPage.getNumber());
        model.addAttribute("totalPages", incidentsPage.getTotalPages());
        model.addAttribute("totalItems", incidentsPage.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("pageSize", size);

        return "host/quan-ly-su-co";
    }

    @GetMapping("/{id}")
    public String viewIncident(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer id,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        try {
            IncidentReports incident = incidentService.getIncidentById(id);
            model.addAttribute("incident", incident);
            model.addAttribute("search", search);
            model.addAttribute("page", page);
            return "host/chi-tiet-quan-ly-su-co";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", "Không tìm thấy khiếu nại");
            return "host/quan-ly-su-co";
        }
    }

    @PostMapping("/{id}/update")
    public String updateIncident(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer id,
            @ModelAttribute IncidentReports updatedIncident,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            RedirectAttributes redirectAttributes) {

        try {
            // Use service layer to update incident (this will trigger notifications and emails)
            incidentService.updateIncident(id, updatedIncident);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật khiếu nại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật trạng thái!");
        }

        return "redirect:/chu-tro/quan-ly-su-co?search=" + search + "&page=" + page;
    }

}
