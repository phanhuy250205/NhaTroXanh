package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;

public interface ContractPdfService {
    byte[] generateContractPdf(ContractDto contractDto) throws Exception;
}
