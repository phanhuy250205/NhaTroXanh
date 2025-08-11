package nhatroxanh.com.Nhatroxanh.Service.Impl;




import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import nhatroxanh.com.Nhatroxanh.Model.Dto.PdfOptions;
import nhatroxanh.com.Nhatroxanh.Service.PdfService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfServiceImpl implements PdfService {

    private static final String PDF_DIRECTORY = "uploads/contracts/";
    private static final String FONT_PATH = "fonts/"; // Nếu cần custom fonts



    @Override
    public String saveContractPdf(String htmlContent, String fileName) {
        try {
            // ✅ CREATE DIRECTORY IF NOT EXISTS
            Path directory = Paths.get(PDF_DIRECTORY);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                System.out.println("📁 Created directory: " + PDF_DIRECTORY);
            }

            // ✅ GENERATE PDF
            byte[] pdfBytes = generateContractPdf(htmlContent);

            // ✅ CREATE FILE PATH WITH TIMESTAMP
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filePath = PDF_DIRECTORY + fileName + "_" + timestamp + ".pdf";

            // ✅ SAVE TO FILE
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(pdfBytes);
            }

            System.out.println("✅ PDF saved successfully: " + filePath);
            return filePath;

        } catch (Exception e) {
            System.err.println("❌ Error saving PDF: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể lưu PDF: " + e.getMessage());
        }
    }

    @Override
    public byte[] generatePdfWithOptions(String htmlContent, PdfOptions options) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // ✅ CREATE PDF WRITER WITH OPTIONS
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);

            // ✅ SET PAGE SIZE
            PageSize pageSize = getPageSize(options.getPageSize(), options.getOrientation());
            pdfDocument.setDefaultPageSize(pageSize);

            // ✅ ENHANCE HTML
            String enhancedHtml = enhanceHtmlForPdf(htmlContent);

            // ✅ CONFIGURE CONVERTER
            ConverterProperties converterProperties = new ConverterProperties();
            converterProperties.setCharset("UTF-8");

            // ✅ CONVERT
            HtmlConverter.convertToPdf(enhancedHtml, pdfDocument, converterProperties);

            return outputStream.toByteArray();

        } catch (Exception e) {
            System.err.println("❌ Error generating PDF with options: " + e.getMessage());
            throw new RuntimeException("Không thể tạo PDF: " + e.getMessage());
        }
    }

    // ✅ ENHANCE HTML FOR BETTER PDF RENDERING
    private String enhanceHtmlForPdf(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("HTML content cannot be null or empty");
        }

        // ✅ ADD CSS FOR BETTER PDF RENDERING
        String pdfCss = """
            <style>
                @page {
                    size: A4;
                    margin: 2cm;
                }
                
                body {
                    font-family: 'DejaVu Sans', Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    font-size: 12px;
                    margin: 0;
                    padding: 0;
                }
                
                .container, .contract-container {
                    max-width: 100%;
                    margin: 0;
                    padding: 0;
                }
                
                .text-center {
                    text-align: center;
                    margin-bottom: 20px;
                }
                
                .row {
                    display: block;
                    margin-top: 30px;
                    page-break-inside: avoid;
                }
                
                .col-6 {
                    width: 45%;
                    display: inline-block;
                    vertical-align: top;
                    text-align: center;
                }
                
                .col-6:first-child {
                    margin-right: 10%;
                }
                
                p {
                    margin: 8px 0;
                    text-align: justify;
                    line-height: 1.5;
                }
                
                strong {
                    font-weight: bold;
                    color: #2c3e50;
                }
                
                h1, h2, h3, h4, h5, h6 {
                    color: #2c3e50;
                    margin: 15px 0 10px 0;
                }
                
                .signature-area {
                    margin-top: 50px;
                    page-break-inside: avoid;
                }
                
                /* Remove problematic CSS */
                .d-flex, .flex-column, .flex-row {
                    display: block !important;
                }
                
                .btn, button {
                    display: none !important;
                }
                
                /* Print-friendly colors */
                .bg-primary, .bg-info, .bg-success, .bg-warning, .bg-danger {
                    background-color: #f8f9fa !important;
                    color: #333 !important;
                }
            </style>
            """;

        // ✅ INSERT CSS INTO HTML
        if (htmlContent.contains("<head>")) {
            htmlContent = htmlContent.replace("</head>", pdfCss + "</head>");
        } else if (htmlContent.contains("<html>")) {
            htmlContent = htmlContent.replace("<html>", "<html><head>" + pdfCss + "</head>");
        } else {
            htmlContent = "<!DOCTYPE html><html><head>" + pdfCss + "</head><body>" + htmlContent + "</body></html>";
        }

        // ✅ CLEAN UP PROBLEMATIC ELEMENTS
        htmlContent = htmlContent
                .replaceAll("(?i)<script[^>]*>.*?</script>", "") // Remove scripts
                .replaceAll("(?i)<link[^>]*stylesheet[^>]*>", "") // Remove external stylesheets
                .replace("flex", "block") // Replace flex with block
                .replace("d-flex", "d-block"); // Bootstrap flex to block

        return htmlContent;
    }

    // ✅ GET PAGE SIZE FROM OPTIONS
    private PageSize getPageSize(String pageSize, String orientation) {
        PageSize size;

        switch (pageSize.toUpperCase()) {
            case "A3":
                size = PageSize.A3;
                break;
            case "A5":
                size = PageSize.A5;
                break;
            case "LETTER":
                size = PageSize.LETTER;
                break;
            default:
                size = PageSize.A4;
        }

        // ✅ ROTATE IF LANDSCAPE
        if ("landscape".equalsIgnoreCase(orientation)) {
            size = size.rotate();
        }

        return size;
    }

    @Override
    public byte[] generateContractPdf(String htmlContent) {
        try {
            // ✅ INPUT VALIDATION
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                throw new IllegalArgumentException("HTML content cannot be null or empty");
            }

            System.out.println("🔄 Starting PDF generation...");
            System.out.println("📝 HTML length: " + htmlContent.length());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String enhancedHtml = enhanceHtmlForPdf(htmlContent);

            System.out.println("🔧 Enhanced HTML length: " + enhancedHtml.length());

            // ✅ CONVERTER PROPERTIES
            ConverterProperties converterProperties = new ConverterProperties();
            converterProperties.setCharset("UTF-8");

            // ✅ CONVERT TO PDF
            HtmlConverter.convertToPdf(enhancedHtml, outputStream, converterProperties);

            byte[] pdfBytes = outputStream.toByteArray();
            outputStream.close();

            // ✅ VALIDATE OUTPUT
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new RuntimeException("PDF generation produced empty output");
            }

            if (pdfBytes.length < 100) {
                throw new RuntimeException("PDF too small, likely corrupted. Size: " + pdfBytes.length);
            }

            String header = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
            if (!header.equals("%PDF")) {
                throw new RuntimeException("Invalid PDF format. Header: " + header);
            }

            System.out.println("✅ PDF generated successfully!");
            System.out.println("📄 Final PDF size: " + pdfBytes.length + " bytes");

            return pdfBytes;

        } catch (Exception e) {
            System.err.println("❌ PDF Generation Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

}

