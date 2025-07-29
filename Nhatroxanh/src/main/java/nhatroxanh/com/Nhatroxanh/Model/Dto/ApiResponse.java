package nhatroxanh.com.Nhatroxanh.Model.Dto;

public class ApiResponse<T> {
    private String status;
    private T data;
    private String error;

    // Constructor full
    public ApiResponse(String status, T data, String error) {
        this.status = status;
        this.data = data;
        this.error = error;
    }

    // Constructor không lỗi
    public ApiResponse(String status, T data) {
        this(status, data, null);
    }

    // Getter (bắt buộc để Jackson serialize)
    public String getStatus() { return status; }
    public T getData() { return data; }
    public String getError() { return error; }
}
