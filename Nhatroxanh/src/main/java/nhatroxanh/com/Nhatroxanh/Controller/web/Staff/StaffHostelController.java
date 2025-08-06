package nhatroxanh.com.Nhatroxanh.Controller.web.Staff;

import java.util.List;

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

import nhatroxanh.com.Nhatroxanh.Model.entity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.entity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;

@Controller
@RequestMapping("/nhan-vien")
public class StaffHostelController {

    @Autowired
    private HostelRepository hostelRepository;

    @GetMapping("/thong-tin-tro")
    public String getHostels(@RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {
        int size = 5;
        Pageable pageable = PageRequest.of(page, size);
        Page<Hostel> hostelPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            try {
                int id = Integer.parseInt(keyword.trim());
                hostelPage = hostelRepository.findByHostelIdOrNameContainingIgnoreCase(id, keyword, pageable);
            } catch (NumberFormatException e) {
                hostelPage = hostelRepository.findByNameContainingIgnoreCase(keyword, pageable);
            }
        } else {
            hostelPage = hostelRepository.findAll(pageable);
        }

        List<Hostel> hostels = hostelPage.getContent();

        long activeHostels = hostels.stream().filter(h -> Boolean.TRUE.equals(h.getStatus())).count();
        long fullHostels = hostels.stream().filter(h -> Boolean.FALSE.equals(h.getStatus())).count();

        model.addAttribute("hostels", hostels);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", hostelPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalHostels", hostelPage.getTotalElements());
        model.addAttribute("activeHostels", activeHostels);
        model.addAttribute("fullHostels", fullHostels);

        return "staff/thong-tin-tro-staff";
    }

    @GetMapping("/chi-tiet-thong-tin-tro/{id}")
    public String viewHostelDetail(@PathVariable("id") Integer id, Model model) {
        Hostel hostel = hostelRepository.findById(id).orElse(null);

        if (hostel == null) {
            return "redirect:/nhan-vien/thong-tin-tro?notfound";
        }

        Users owner = hostel.getOwner();
        List<Rooms> rooms = hostel.getRooms();

        model.addAttribute("hostel", hostel);
        model.addAttribute("owner", owner);
        model.addAttribute("rooms", rooms);

        return "staff/detail-thong-tin-tro-staff";
    }

    @PostMapping("/cap-nhat-trang-thai-tro")
    public String updateHostelStatus(@RequestParam("hostelId") Integer hostelId,
            @RequestParam("status") Boolean status,
            RedirectAttributes redirectAttributes) {
        boolean updated = hostelRepository.findById(hostelId).map(hostel -> {
            hostel.setStatus(status);
            hostelRepository.save(hostel);
            return true;
        }).orElse(false);

        if (updated) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khu trọ!");
        }

        return "redirect:/nhan-vien/chi-tiet-thong-tin-tro/" + hostelId;

    }

}
