package nhatroxanh.com.Nhatroxanh.Service;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantDetailDTO;
import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantInfoDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.IncidentReports;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

public interface TenantService {
        Page<TenantInfoDTO> getTenantsForOwner(Integer ownerId, String keyword, Integer hostelId,
                        Contracts.Status status,
                        Pageable pageable);

        Map<String, Long> getContractStatusStats(Integer ownerId);

        List<Hostel> getHostelsForOwner(Integer ownerId);

        Page<TenantInfoDTO> findAllForTesting(Pageable pageable);

        void updateContractStatus(Integer contractId, Boolean newStatus);

        TenantDetailDTO getTenantDetailByContractId(Integer contractId);

        List<Contracts> getActiveContracts();

        Page<Contracts> getContractHistory(Pageable pageable);

        Contracts getContractById(Integer contractId);

        Map<String, Object> getQuickStats(Pageable pageable);

        void returnRoom(Integer contractId, Date returnDate, String reason);

        void submitReview(Integer roomId, Double rating, String comment);

        List<IncidentReports> getIncidentReportsByRoom(Integer roomId);

        void submitIncidentReport(Integer roomId, String incidentType, String description,
                        IncidentReports.IncidentLevel level);

        void extendContract(Integer contractId, Date newEndDate, String message);

        Set<Utility> getUtilitiesByRoomId(Integer roomId);

        void createIncidentReport(Integer roomId, String incidentType, String description, String level,
                        List<MultipartFile> images, CustomUserDetails userDetails) throws Exception;

        void updateIncidentReport(Integer reportId, Integer roomId, String incidentType,
                        String description, String level, List<MultipartFile> images,
                        List<Integer> imageIdsToDelete,
                        CustomUserDetails userDetails) throws IOException;

        void createExtensionRequest(Integer contractId, LocalDate requestedExtendDate, String message, Users tenant);
}
