package nhatroxanh.com.Nhatroxanh.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractEmailDto {
    private Long contractId;
    private String tenantName;
    private String recipientEmail;
    private String contractFilePath;
    private String roomNumber;
    private String contractType;
    private String hotline;
    private String supportEmail;
    private Double monthlyRent;
    private String startDate;
    private String endDate;

    // Builder pattern
    public static ContractEmailDtoBuilder builder() {
        return new ContractEmailDtoBuilder();
    }

    public static class ContractEmailDtoBuilder {
        private ContractEmailDto dto = new ContractEmailDto();

        public ContractEmailDtoBuilder contractId(Long contractId) {
            dto.setContractId(contractId);
            return this;
        }

        public ContractEmailDtoBuilder tenantName(String tenantName) {
            dto.setTenantName(tenantName);
            return this;
        }

        public ContractEmailDtoBuilder recipientEmail(String recipientEmail) {
            dto.setRecipientEmail(recipientEmail);
            return this;
        }

        public ContractEmailDtoBuilder contractFilePath(String contractFilePath) {
            dto.setContractFilePath(contractFilePath);
            return this;
        }

        public ContractEmailDtoBuilder roomNumber(String roomNumber) {
            dto.setRoomNumber(roomNumber);
            return this;
        }

        public ContractEmailDtoBuilder hotline(String hotline) {
            dto.setHotline(hotline);
            return this;
        }

        public ContractEmailDtoBuilder supportEmail(String supportEmail) {
            dto.setSupportEmail(supportEmail);
            return this;
        }

        public ContractEmailDto build() {
            return dto;
        }
    }
}
