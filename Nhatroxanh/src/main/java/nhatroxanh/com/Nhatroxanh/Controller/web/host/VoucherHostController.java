package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import nhatroxanh.com.Nhatroxanh.Service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/chu-tro/voucher")
public class VoucherHostController {

    @Autowired
    private VoucherService voucherService;

    @GetMapping
    public String voucher() {
        return "host/voucher-host";
    }

}