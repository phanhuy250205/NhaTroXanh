// Trong package DTO của bạn, ví dụ: model/dto/UnregisteredTenantsDTO.java
package nhatroxanh.com.Nhatroxanh.Model.Dto;

import lombok.Data;
import java.util.Date;

@Data
public class UnregisteredTenantsDTO {
    private String fullName;
    private String phone;
    private Date birthday;
    private String cccdNumber;
    private Date issueDate;
    private String issuePlace;
    private String email;
    private String street;
    private String ward;
    private String district;
    private String province;
}