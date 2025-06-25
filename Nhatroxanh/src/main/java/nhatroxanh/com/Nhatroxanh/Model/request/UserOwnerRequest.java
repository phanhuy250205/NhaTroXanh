package nhatroxanh.com.Nhatroxanh.Model.request;

import lombok.Data;

@Data
public class UserOwnerRequest {
    private String fullName;
    private String email;
    private String password;
    private String phoneNumber;
    private String birthDate; 
}
