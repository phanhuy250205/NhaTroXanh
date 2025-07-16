package nhatroxanh.com.Nhatroxanh.Model.enity;

public enum RoomStatus {
    active, // Đã thuê
    unactive, // Trống
    repair; // Bảo trì

    public static RoomStatus fromString(String status) {
        if (status == null) return RoomStatus.unactive;
        try {
            return RoomStatus.valueOf(status.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            return RoomStatus.unactive;
        }
    }

    @Override
    public String toString() {
        return name();
    }
}
