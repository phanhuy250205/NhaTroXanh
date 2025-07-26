package nhatroxanh.com.Nhatroxanh.Util;

import org.springframework.stereotype.Component;

@Component
public class CccdUtils {

    public  String maskCccd(String cccdNumber) {
        if (cccdNumber == null || cccdNumber.length() < 6) {
            return "************";
        }
        return cccdNumber.substring(0, 3) + "****" + cccdNumber.substring(cccdNumber.length() - 3);
    }
}