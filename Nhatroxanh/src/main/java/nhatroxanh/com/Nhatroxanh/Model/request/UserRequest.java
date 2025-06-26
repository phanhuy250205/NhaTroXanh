package nhatroxanh.com.Nhatroxanh.Model.request;

import lombok.Data;

@Data
public class UserRequest {
   private String fullName;
    private String email;
    private String password;
    private String phoneNumber;
    public Object getUsername() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUsername'");
    }
}
