package nhatroxanh.com.Nhatroxanh.Service.Impl;

import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.ConverterProperties;
import nhatroxanh.com.Nhatroxanh.Service.ContractPdfService;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ContractPdfServiceImpl implements ContractPdfService {

    @Override
    public byte[] generateContractPdf(ContractDto contractDto) throws Exception {
        try {
            // Tạo HTML content từ ContractDto
            String htmlContent = buildContractHtml(contractDto);

            // Chuyển HTML thành PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ConverterProperties properties = new ConverterProperties();
            HtmlConverter.convertToPdf(htmlContent, outputStream, properties);

            return outputStream.toByteArray();

        } catch (Exception e) {
            System.err.println("❌ Error generating PDF: " + e.getMessage());
            throw new Exception("Không thể tạo PDF hợp đồng: " + e.getMessage(), e);
        }
    }

    private String buildContractHtml(ContractDto contractDto) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        DecimalFormat formatter = new DecimalFormat("#,###");

        // Lấy thông tin từ ContractDto
        String landlordName = contractDto.getOwner().getFullName() != null ? contractDto.getOwner().getFullName() : "";
        String landlordPhone = contractDto.getOwner().getPhone() != null ? contractDto.getOwner().getPhone() : "";
        String landlordId = contractDto.getOwner().getCccdNumber() != null ? contractDto.getOwner().getCccdNumber() : "";
        String landlordAddress = contractDto.getOwnerAddress() != null ? contractDto.getOwnerAddress() : "";

        // Xử lý tenant (có thể là registered hoặc unregistered)
        String tenantName = "";
        String tenantPhone = "";
        String tenantId = "";
        String tenantAddress = "";

        if ("REGISTERED".equals(contractDto.getTenantType()) && contractDto.getTenant() != null) {
            tenantName = contractDto.getTenant().getFullName() != null ? contractDto.getTenant().getFullName() : "";
            tenantPhone = contractDto.getTenant().getPhone() != null ? contractDto.getTenant().getPhone() : "";
            tenantId = contractDto.getTenant().getCccdNumber() != null ? contractDto.getTenant().getCccdNumber() : "";
            tenantAddress = contractDto.getTenantAddress() != null ? contractDto.getTenantAddress() : "";
        } else if ("UNREGISTERED".equals(contractDto.getTenantType()) && contractDto.getUnregisteredTenant() != null) {
            tenantName = contractDto.getUnregisteredTenant().getFullName() != null ? contractDto.getUnregisteredTenant().getFullName() : "";
            tenantPhone = contractDto.getUnregisteredTenant().getPhone() != null ? contractDto.getUnregisteredTenant().getPhone() : "";
            tenantId = contractDto.getUnregisteredTenant().getCccdNumber() != null ? contractDto.getUnregisteredTenant().getCccdNumber() : "";
            tenantAddress = contractDto.getTenantAddress() != null ? contractDto.getTenantAddress() : "";
        }

        String roomAddress = contractDto.getRoom().getAddress() != null ? contractDto.getRoom().getAddress() : "";
        String contractNumber = "HD-" + (contractDto.getId() != null ? contractDto.getId().toString() : "001");
        String formattedRent = contractDto.getTerms().getPrice() != null ?
                formatter.format(contractDto.getTerms().getPrice()) : "0";
        String startDate = contractDto.getTerms().getStartDate() != null ?
                contractDto.getTerms().getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String endDate = contractDto.getTerms().getEndDate() != null ?
                contractDto.getTerms().getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" +
                "body{font-family:Arial,sans-serif;margin:40px;line-height:1.5;color:#333}" +
                ".header{text-align:center;margin-bottom:30px;border-bottom:2px solid #333;padding-bottom:20px}" +
                ".title{font-size:24px;font-weight:bold;text-align:center;margin:20px 0;color:#d32f2f}" +
                ".contract-number{text-align:center;font-style:italic;margin-bottom:20px}" +
                ".section{margin:20px 0;padding:15px;background:#f9f9f9;border-left:4px solid #2196f3}" +
                ".section-title{font-weight:bold;font-size:16px;margin-bottom:10px;color:#1976d2}" +
                ".info-row{margin:8px 0;padding:5px 0}" +
                ".info-label{display:inline-block;width:150px;font-weight:500}" +
                ".info-value{font-weight:bold;color:#333}" +
                ".terms-section{margin:20px 0;padding:15px;background:#fff3e0;border:1px solid #ff9800}" +
                ".terms-title{font-weight:bold;font-size:16px;margin-bottom:15px;color:#f57c00}" +
                ".terms-list{margin:10px 0}" +
                ".terms-item{margin:8px 0;padding-left:20px;position:relative}" +
                ".terms-item:before{content:'•';position:absolute;left:0;color:#ff9800;font-weight:bold}" +
                ".signature{margin-top:60px;display:table;width:100%}" +
                ".signature-left,.signature-right{display:table-cell;width:50%;text-align:center;vertical-align:top}" +
                ".signature-box{border:1px solid #ddd;padding:20px;margin:10px;min-height:120px}" +
                ".signature-title{font-weight:bold;margin-bottom:10px;color:#1976d2}" +
                ".signature-note{font-style:italic;color:#666;font-size:14px}" +
                ".date-location{text-align:center;margin:30px 0;font-style:italic;color:#666}" +
                "</style></head><body>" +

                "<div class='header'>" +
                "<h3>CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</h3>" +
                "<p><strong>Độc lập - Tự do - Hạnh phúc</strong></p>" +
                "</div>" +

                "<div class='title'>HỢP ĐỒNG THUÊ PHÒNG TRỌ</div>" +
                "<div class='contract-number'>Số: " + contractNumber + "</div>" +

                "<div class='section'>" +
                "<div class='section-title'>BÊN CHO THUÊ (Bên A)</div>" +
                "<div class='info-row'><span class='info-label'>Họ và tên:</span> <span class='info-value'>" + landlordName + "</span></div>" +
                "<div class='info-row'><span class='info-label'>CMND/CCCD:</span> <span class='info-value'>" + landlordId + "</span></div>" +
                "<div class='info-row'><span class='info-label'>Số điện thoại:</span> <span class='info-value'>" + landlordPhone + "</span></div>" +
                "<div class='info-row'><span class='info-label'>Địa chỉ:</span> <span class='info-value'>" + landlordAddress + "</span></div>" +
                "</div>" +

                "<div class='section'>" +
                "<div class='section-title'>BÊN THUÊ (Bên B)</div>" +
                "<div class='info-row'><span class='info-label'>Họ và tên:</span> <span class='info-value'>" + tenantName + "</span></div>" +
                "<div class='info-row'><span class='info-label'>CMND/CCCD:</span> <span class='info-value'>" + tenantId + "</span></div>" +
                "<div class='info-row'><span class='info-label'>Số điện thoại:</span> <span class='info-value'>" + tenantPhone + "</span></div>" +
                "<div class='info-row'><span class='info-label'>Địa chỉ:</span> <span class='info-value'>" + tenantAddress + "</span></div>" +
                "</div>" +

                "<div class='section'>" +
                "<div class='section-title'>THÔNG TIN PHÒNG TRỌ</div>" +
                "<div class='info-row'><span class='info-label'>Địa chỉ phòng:</span> <span class='info-value'>" + roomAddress + "</span></div>" +
                "<div class='info-row'><span class='info-label'>Giá thuê/tháng:</span> <span class='info-value'>" + formattedRent + " VNĐ</span></div>" +
                "<div class='info-row'><span class='info-label'>Ngày bắt đầu:</span> <span class='info-value'>" + startDate + "</span></div>" +
                "<div class='info-row'><span class='info-label'>Ngày kết thúc:</span> <span class='info-value'>" + endDate + "</span></div>" +
                "</div>" +

                "<div class='terms-section'>" +
                "<div class='terms-title'>ĐIỀU KHOẢN HỢP ĐỒNG</div>" +
                "<div class='terms-list'>" +
                "<div class='terms-item'>Bên B có trách nhiệm thanh toán tiền thuê đúng hạn vào đầu mỗi tháng</div>" +
                "<div class='terms-item'>Bên B có trách nhiệm giữ gìn tài sản và vệ sinh chung của khu vực</div>" +
                "<div class='terms-item'>Không được sử dụng phòng vào mục đích bất hợp pháp</div>" +
                "<div class='terms-item'>Khi chấm dứt hợp đồng, Bên B phải bàn giao lại phòng trong tình trạng ban đầu</div>" +
                "<div class='terms-item'>Hai bên có thể thỏa thuận sửa đổi, bổ sung hợp đồng bằng văn bản</div>" +
                "</div>" +
                "</div>" +

                "<div class='date-location'>Hà Nội, ngày " + currentDate + "</div>" +

                "<div class='signature'>" +
                "<div class='signature-left'>" +
                "<div class='signature-box'>" +
                "<div class='signature-title'>BÊN CHO THUÊ</div>" +
                "<div class='signature-note'>(Ký và ghi rõ họ tên)</div>" +
                "<br><br><br>" +
                "<div style='font-weight:bold'>" + landlordName + "</div>" +
                "</div>" +
                "</div>" +
                "<div class='signature-right'>" +
                "<div class='signature-box'>" +
                "<div class='signature-title'>BÊN THUÊ</div>" +
                "<div class='signature-note'>(Ký và ghi rõ họ tên)</div>" +
                "<br><br><br>" +
                "<div style='font-weight:bold'>" + tenantName + "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "</body></html>";
    }
}
