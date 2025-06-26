package nhatroxanh.com.Nhatroxanh.Model.Dto;

import java.time.LocalDate;
import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class HostInfoDTO {
    private String fullname;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthday;

    private String cccd;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date issueDate;

    private String issuePlace;

    private Boolean gender;

    private String email;

    private String phone;

    private String address;

    private String avatar;
    
    private MultipartFile avatarFile;
}
