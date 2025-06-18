package nhatroxanh.com.Nhatroxanh.Model.request;

import lombok.Data;

@Data
public class UserRequest {
   private String fullName;
    private String username; 
    private String email;
    private String password;
    private String phoneNumber;
}
