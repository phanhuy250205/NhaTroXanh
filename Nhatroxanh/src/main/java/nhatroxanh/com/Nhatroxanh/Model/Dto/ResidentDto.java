package nhatroxanh.com.Nhatroxanh.Model.Dto;

public class ResidentDto {
      private String fullName;
    private String birthYear;
    private String phone;
    private String cccdNumber;

    
    public ResidentDto() {
    }


    public ResidentDto(String fullName, String birthYear, String phone, String cccdNumber) {
        this.fullName = fullName;
        this.birthYear = birthYear;
        this.phone = phone;
        this.cccdNumber = cccdNumber;
    }


    public String getFullName() {
        return fullName;
    }


    public void setFullName(String fullName) {
        this.fullName = fullName;
    }


    public String getBirthYear() {
        return birthYear;
    }


    public void setBirthYear(String birthYear) {
        this.birthYear = birthYear;
    }


    public String getPhone() {
        return phone;
    }


    public void setPhone(String phone) {
        this.phone = phone;
    }


    public String getCccdNumber() {
        return cccdNumber;
    }


    public void setCccdNumber(String cccdNumber) {
        this.cccdNumber = cccdNumber;
    }

    
}
