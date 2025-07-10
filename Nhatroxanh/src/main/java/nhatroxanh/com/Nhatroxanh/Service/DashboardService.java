package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.PaymentsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ContractsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private RoomsRepository roomsRepository;

    @Autowired
    private PaymentsRepository paymentsRepository;

    @Autowired
    private ContractsRepository contractsRepository;

    public Map<String, Object> getDashboardStats(Integer ownerId, String timeRange) {
        Map<String, Object> stats = new HashMap<>();
        LocalDate currentDate = LocalDate.now();
        LocalDate endDate = currentDate;
        LocalDate startDate;
        String chartType = "day";

        // Thống kê nhà trọ và phòng
        stats.put("totalHostels", hostelRepository.countHostelsByOwnerId(ownerId));
        stats.put("totalRooms", roomsRepository.countRoomsByOwnerId(ownerId));
        stats.put("vacantRooms", roomsRepository.countVacantRoomsByOwnerId(ownerId));
        stats.put("rentedRooms", roomsRepository.countRentedRoomsByOwnerId(ownerId));
        stats.put("depositedRooms", roomsRepository.countDepositedRoomsByOwnerId(ownerId));
        stats.put("overdueRooms", paymentsRepository.countOverdueRoomsByOwnerId(ownerId));

        // Thống kê người thuê
        stats.put("totalTenants", contractsRepository.countActiveTenantsByOwnerId(ownerId, Date.valueOf(currentDate)));
        stats.put("expiringContracts", contractsRepository.countExpiringContractsByOwnerId(
                ownerId, Date.valueOf(currentDate), Date.valueOf(currentDate.plusDays(30))));

        // Xử lý khoảng thời gian
        if (timeRange.startsWith("month-")) {
            String[] parts = timeRange.split("-");
            int year = Integer.parseInt(parts[1]);
            int month = Integer.parseInt(parts[2]);
            startDate = LocalDate.of(year, month, 1);
            endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            chartType = "day";
        } else if (timeRange.startsWith("quarter-")) {
            String[] parts = timeRange.split("-");
            int year = Integer.parseInt(parts[1]);
            int quarter = Integer.parseInt(parts[2]);
            startDate = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
            endDate = startDate.plusMonths(3).minusDays(1);
            chartType = "month";
        } else if (timeRange.startsWith("year-")) {
            int year = Integer.parseInt(timeRange.split("-")[1]);
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
            chartType = "quarter";
        } else {
            switch (timeRange) {
                case "7 ngày gần đây":
                    startDate = endDate.minusDays(7);
                    chartType = "day";
                    break;
                case "14 ngày gần đây":
                    startDate = endDate.minusDays(14);
                    chartType = "day";
                    break;
                case "30 ngày gần đây":
                    startDate = endDate.minusDays(30);
                    chartType = "day";
                    break;
                case "3 tháng gần đây":
                    startDate = endDate.minusMonths(3);
                    chartType = "month";
                    break;
                case "6 tháng gần đây":
                    startDate = endDate.minusMonths(6);
                    chartType = "month";
                    break;
                case "1 năm gần đây":
                    startDate = endDate.minusYears(1);
                    chartType = "month";
                    break;
                default:
                    startDate = endDate.minusDays(7);
                    chartType = "day";
            }
        }

        // Thống kê người thuê mới trong khoảng thời gian
        stats.put("newTenants", contractsRepository.countNewTenantsByOwnerIdAndDateRange(
                ownerId, Date.valueOf(startDate), Date.valueOf(endDate)));

        // Tính tổng doanh thu
        Float revenue = paymentsRepository.sumRevenueByOwnerIdAndDateRange(
                ownerId, Date.valueOf(startDate), Date.valueOf(endDate));
        stats.put("totalRevenue", revenue != null ? revenue : 0.0f);

        // Lấy dữ liệu cho biểu đồ
        List<String> labels = new ArrayList<>();
        List<Float> revenueData = new ArrayList<>();

        if (chartType.equals("day")) {
            List<Object[]> dailyRevenue = paymentsRepository.getDailyRevenueByOwnerIdAndDateRange(
                    ownerId, Date.valueOf(startDate), Date.valueOf(endDate));
            for (Object[] row : dailyRevenue) {
                labels.add(row[0].toString());
                revenueData.add(((Number) row[1]).floatValue());
            }
        } else if (chartType.equals("month")) {
            List<Object[]> monthlyRevenue = paymentsRepository.getMonthlyRevenueByOwnerIdAndDateRange(
                    ownerId, Date.valueOf(startDate), Date.valueOf(endDate));
            for (Object[] row : monthlyRevenue) {
                labels.add("Tháng " + row[0].toString());
                revenueData.add(((Number) row[1]).floatValue());
            }
        } else if (chartType.equals("quarter")) {
            List<Object[]> quarterlyRevenue = paymentsRepository.getQuarterlyRevenueByOwnerIdAndDateRange(
                    ownerId, Date.valueOf(startDate), Date.valueOf(endDate));
            for (Object[] row : quarterlyRevenue) {
                labels.add("Quý " + row[0].toString());
                revenueData.add(((Number) row[1]).floatValue());
            }
        }

        stats.put("chartLabels", labels);
        stats.put("chartRevenue", revenueData);

        return stats;
    }
}