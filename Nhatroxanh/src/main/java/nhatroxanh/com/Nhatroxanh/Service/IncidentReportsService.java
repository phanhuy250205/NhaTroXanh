package nhatroxanh.com.Nhatroxanh.Service;

import org.springframework.data.domain.Page;

import nhatroxanh.com.Nhatroxanh.Model.entity.IncidentReports;

public interface IncidentReportsService {
    Page<IncidentReports> getIncidentsByHostId(Integer hostId, String search, int page, int size);

    IncidentReports getIncidentById(Integer id);

    void updateIncident(Integer id, IncidentReports updatedIncident);
}