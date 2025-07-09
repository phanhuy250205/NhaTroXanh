package nhatroxanh.com.Nhatroxanh.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments.PaymentStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.RoomStatus;
import nhatroxanh.com.Nhatroxanh.Repository.ContractsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.IncidentReportsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;

@Service
public class DashboardAdminService {
        @Autowired
        private ContractsRepository contractsRepository;

        @Autowired
        private RoomsRepository roomsRepository;

        @Autowired
        private UserRepository usersRepository;

        @Autowired
        private IncidentReportsRepository incidentReportsRepository;

        @PersistenceContext
        private EntityManager entityManager;

        // DTO để trả về dữ liệu thống kê tổng quan
        public static class DashboardStats {
                private BigDecimal totalRevenue; // Doanh thu tổng (VNĐ)
                private long totalRooms; // Tổng số phòng
                private long occupiedRooms; // Phòng đã thuê
                private long vacantRooms; // Phòng còn trống
                private long maintenanceRooms; // Phòng bảo trì
                private long totalUsers; // Tổng số người dùng
                private long totalContracts; // Tổng số hợp đồng
                private long totalIncidents; // Tổng số khiếu nại/sự cố
                private Map<String, Object> revenueChart; // Dữ liệu biểu đồ doanh thu
                private Map<String, Long> occupancyChart; // Dữ liệu tỷ lệ lấp đầy phòng
                private Map<String, Long> userGrowthChart; // Dữ liệu tăng trưởng người dùng
                private Map<String, Long> complaintsChart; // Dữ liệu phân bố khiếu nại

                // Constructor, getters, setters
                public DashboardStats() {
                        this.revenueChart = new HashMap<>();
                        this.occupancyChart = new HashMap<>();
                        this.userGrowthChart = new HashMap<>();
                        this.complaintsChart = new HashMap<>();
                }

                // Getters and setters
                public BigDecimal getTotalRevenue() {
                        return totalRevenue;
                }

                public void setTotalRevenue(BigDecimal totalRevenue) {
                        this.totalRevenue = totalRevenue;
                }

                public long getTotalRooms() {
                        return totalRooms;
                }

                public void setTotalRooms(long totalRooms) {
                        this.totalRooms = totalRooms;
                }

                public long getOccupiedRooms() {
                        return occupiedRooms;
                }

                public void setOccupiedRooms(long occupiedRooms) {
                        this.occupiedRooms = occupiedRooms;
                }

                public long getVacantRooms() {
                        return vacantRooms;
                }

                public void setVacantRooms(long vacantRooms) {
                        this.vacantRooms = vacantRooms;
                }

                public long getMaintenanceRooms() {
                        return maintenanceRooms;
                }

                public void setMaintenanceRooms(long maintenanceRooms) {
                        this.maintenanceRooms = maintenanceRooms;
                }

                public long getTotalUsers() {
                        return totalUsers;
                }

                public void setTotalUsers(long totalUsers) {
                        this.totalUsers = totalUsers;
                }

                public long getTotalContracts() {
                        return totalContracts;
                }

                public void setTotalContracts(long totalContracts) {
                        this.totalContracts = totalContracts;
                }

                public long getTotalIncidents() {
                        return totalIncidents;
                }

                public void setTotalIncidents(long totalIncidents) {
                        this.totalIncidents = totalIncidents;
                }

                public Map<String, Object> getRevenueChart() {
                        return revenueChart;
                }

                public void setRevenueChart(Map<String, Object> revenueChart) {
                        this.revenueChart = revenueChart;
                }

                public Map<String, Long> getOccupancyChart() {
                        return occupancyChart;
                }

                public void setOccupancyChart(Map<String, Long> occupancyChart) {
                        this.occupancyChart = occupancyChart;
                }

                public Map<String, Long> getUserGrowthChart() {
                        return userGrowthChart;
                }

                public void setUserGrowthChart(Map<String, Long> userGrowthChart) {
                        this.userGrowthChart = userGrowthChart;
                }

                public Map<String, Long> getComplaintsChart() {
                        return complaintsChart;
                }

                public void setComplaintsChart(Map<String, Long> complaintsChart) {
                        this.complaintsChart = complaintsChart;
                }
        }

        // Lấy thống kê tổng quan
        public DashboardStats getDashboardStatistics() {
                DashboardStats stats = new DashboardStats();

                // 1. Doanh thu tổng (từ payments đã thanh toán)
                stats.setTotalRevenue(calculateTotalRevenue());

                // 2. Tổng số phòng và trạng thái
                stats.setTotalRooms(roomsRepository.count());
                stats.setOccupiedRooms(roomsRepository.countByStatus(RoomStatus.active));
                stats.setVacantRooms(roomsRepository.countByStatus(RoomStatus.unactive));
                stats.setMaintenanceRooms(roomsRepository.countByStatus(RoomStatus.repair));

                // 3. Tổng số người dùng
                stats.setTotalUsers(usersRepository.count());

                // 4. Tổng số hợp đồng
                stats.setTotalContracts(contractsRepository.countByStatus(Contracts.Status.ACTIVE));

                // 5. Tổng số khiếu nại/sự cố
                stats.setTotalIncidents(incidentReportsRepository.count());

                // 6. Biểu đồ doanh thu 12 tháng
                stats.setRevenueChart(getRevenueChartData());

                // 7. Biểu đồ tỷ lệ lấp đầy phòng
                stats.setOccupancyChart(getOccupancyChartData());

                // 8. Biểu đồ tăng trưởng người dùng
                stats.setUserGrowthChart(getUserGrowthChartData());

                // 9. Biểu đồ phân bố khiếu nại
                stats.setComplaintsChart(getComplaintsChartData());

                return stats;
        }

        // Tính doanh thu tổng từ các khoản thanh toán đã hoàn thành
        private BigDecimal calculateTotalRevenue() {
                String query = "SELECT SUM(p.total_amount) FROM payments p WHERE p.payment_status = :status";
                Query nativeQuery = entityManager.createNativeQuery(query);
                nativeQuery.setParameter("status", PaymentStatus.ĐÃ_THANH_TOÁN.name());

                Object result = nativeQuery.getSingleResult();

                BigDecimal value;
                if (result == null) {
                        value = BigDecimal.ZERO;
                } else if (result instanceof BigDecimal) {
                        value = (BigDecimal) result;
                } else if (result instanceof Number) {
                        value = BigDecimal.valueOf(((Number) result).doubleValue());
                } else {
                        throw new IllegalStateException("Unexpected result type: " + result.getClass());
                }

                // ✅ Chuyển đổi sang triệu VND
                return value.divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.HALF_UP);
        }

        // Dữ liệu biểu đồ doanh thu 12 tháng
        private Map<String, Object> getRevenueChartData() {
                Map<String, Object> chartData = new HashMap<>();
                List<String> labels = new ArrayList<>();
                List<BigDecimal> data = new ArrayList<>();

                LocalDate now = LocalDate.now();
                for (int i = 11; i >= 0; i--) {
                        LocalDate month = now.minusMonths(i);
                        labels.add("T" + month.getMonthValue());

                        String query = "SELECT SUM(p.total_amount) FROM payments p " +
                                        "WHERE p.payment_status = :status " +
                                        "AND MONTH(p.payment_date) = :month " +
                                        "AND YEAR(p.payment_date) = :year";
                        Query nativeQuery = entityManager.createNativeQuery(query);
                        nativeQuery.setParameter("status", PaymentStatus.ĐÃ_THANH_TOÁN.name());
                        nativeQuery.setParameter("month", month.getMonthValue());
                        nativeQuery.setParameter("year", month.getYear());

                        Object result = nativeQuery.getSingleResult();
                        BigDecimal revenue = result != null
                                        ? BigDecimal.valueOf(((Number) result).doubleValue())
                                        : BigDecimal.ZERO;

                        data.add(revenue);
                }

                chartData.put("labels", labels);
                chartData.put("data", data);
                return chartData;
        }

        private Map<String, Long> getOccupancyChartData() {
                Map<String, Long> chartData = new HashMap<>();
                chartData.put("Đã thuê", roomsRepository.countByStatus(RoomStatus.active));
                chartData.put("Còn trống", roomsRepository.countByStatus(RoomStatus.unactive));
                chartData.put("Bảo trì", roomsRepository.countByStatus(RoomStatus.repair));
                return chartData;
        }

        private Map<String, Long> getUserGrowthChartData() {
                Map<String, Long> chartData = new HashMap<>();
                LocalDate now = LocalDate.now();

                for (int i = 5; i >= 0; i--) {
                        LocalDate month = now.minusMonths(i);
                        String label = "T" + month.getMonthValue();

                        String query = "SELECT COUNT(*) FROM users u WHERE MONTH(u.created_at) = :month AND YEAR(u.created_at) = :year";
                        Query nativeQuery = entityManager.createNativeQuery(query);
                        nativeQuery.setParameter("month", month.getMonthValue());
                        nativeQuery.setParameter("year", month.getYear());
                        Long count = ((Number) nativeQuery.getSingleResult()).longValue();
                        chartData.put(label, count);
                }

                return chartData;
        }

        // Dữ liệu biểu đồ phân bố khiếu nại theo loại
        private Map<String, Long> getComplaintsChartData() {
                String query = "SELECT i.incident_type, COUNT(*) FROM incident_reports i GROUP BY i.incident_type";
                Query nativeQuery = entityManager.createNativeQuery(query);
                List<Object[]> results = nativeQuery.getResultList();

                Map<String, Long> chartData = new HashMap<>();
                for (Object[] result : results) {
                        String incidentType = (String) result[0];
                        Long count = ((Number) result[1]).longValue();
                        chartData.put(incidentType, count);
                }

                // Thêm các loại khiếu nại mặc định nếu không có dữ liệu
                chartData.putIfAbsent("Điện nước", 0L);
                chartData.putIfAbsent("Tiếng ồn", 0L);
                chartData.putIfAbsent("Vệ sinh", 0L);
                chartData.putIfAbsent("An ninh", 0L);
                chartData.putIfAbsent("Khác", 0L);

                return chartData;
        }
}