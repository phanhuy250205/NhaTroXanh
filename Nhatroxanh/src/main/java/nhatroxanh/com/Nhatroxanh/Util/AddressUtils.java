package nhatroxanh.com.Nhatroxanh.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public class AddressUtils {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AddressUtils.class);

    public static Map<String, String> parseAddress(String address) {
        Map<String, String> parsed = new HashMap<>();
        parsed.put("street", "Chưa cập nhật");
        parsed.put("ward", "");
        parsed.put("district", "");
        parsed.put("province", "");

        if (!StringUtils.hasText(address)) {
            return parsed;
        }

        address = address.toLowerCase().trim().replaceAll("\\s+,\\s*", ", ").replaceAll("\\s+", " ");
        // Loại "phòng [tên] " đầu
        address = address.replaceAll("^phòng\\s+trọ?\\s*", "").replaceAll("^phòng\\s+", "");

        String[] parts = address.split(",");
        int len = parts.length;
        if (len > 0) parsed.put("street", capitalize(parts[0].trim()));  // Street luôn phần đầu
        if (len > 1) parsed.put("ward", capitalize(parts[1].trim().replace("phường ", "Phường ").replace("xã ", "Xã ")));
        if (len > 2) parsed.put("district", capitalize(parts[2].trim().replace("quận ", "Quận ").replace("huyện ", "Huyện ")));
        if (len > 3) parsed.put("province", capitalize(parts[3].trim().replace("tỉnh ", "Tỉnh ").replace("thành phố ", "Thành phố ")));
        else if (len > 0) parsed.put("province", capitalize(parts[len-1].trim().replace("đà nẵng", "Đà Nẵng")));

        // Thêm "Số " nếu street là số/chữ cái ngắn
        String street = parsed.get("street");
        if (street.matches("^\\s*[A-Za-z0-9]+\\s*$")) {
            parsed.put("street", "Số " + street);
        }

        logger.info("Final parsed for address '{}': {}", address, parsed);  // Log để debug
        return parsed;
    }

    private static String capitalize(String str) {
        return StringUtils.capitalize(str);
    }
}