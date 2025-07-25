package nhatroxanh.com.Nhatroxanh.Service;



import nhatroxanh.com.Nhatroxanh.Model.Dto.PdfOptions;

import java.io.ByteArrayOutputStream;

public interface PdfService {
    /**
     * Generate PDF from HTML content
     * @param htmlContent HTML string
     * @return PDF as byte array
     */
    byte[] generateContractPdf(String htmlContent);

    /**
     * Save PDF to file system
     * @param htmlContent HTML string
     * @param fileName file name without extension
     * @return file path
     */
    String saveContractPdf(String htmlContent, String fileName);

    /**
     * Generate PDF with custom options
     * @param htmlContent HTML string
     * @param options PDF generation options
     * @return PDF as byte array
     */
    byte[] generatePdfWithOptions(String htmlContent, PdfOptions options);


}
