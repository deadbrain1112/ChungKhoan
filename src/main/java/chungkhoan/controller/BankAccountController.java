package chungkhoan.controller;

import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.service.TaiKhoanNganHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/bank-accounts")
public class BankAccountController {

    @Autowired
    private TaiKhoanNganHangService taiKhoanNganHangService;

    @GetMapping("/by-investor")
    @ResponseBody
    public List<TaiKhoanNganHangResponse> getByInvestor(@RequestParam String maNDT) {
        List<TaiKhoanNganHang> accounts = bankAccountService.getByMaNDT(maNDT);

        return accounts.stream().map(tk -> {
            var nh = tk.getNganHang();
            return TaiKhoanNganHangResponse.builder()
                    .maTK(tk.getMaTK())
                    .soTien(tk.getSoTien())
                    .maNH(nh != null ? nh.getMaNH() : null)
                    .tenNH(nh != null ? nh.getTenNH() : null)
                    .diaChi(nh != null ? nh.getDiaChi() : null)
                    .phone(nh != null ? nh.getPhone() : null)
                    .email(nh != null ? nh.getEmail() : null)
                    .build();
        }).toList();
    }

}
