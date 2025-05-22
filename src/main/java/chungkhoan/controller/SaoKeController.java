package chungkhoan.controller;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import chungkhoan.entity.LenhDat;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.service.LenhDatService;
import jakarta.servlet.http.HttpSession;

@Controller
public class SaoKeController {

    @Autowired
    private LenhDatService lenhDatService;

    @PostMapping("/nhadautu/sao-ke-gdck")
    public String hienThiLenhDatTheoNDT(@RequestParam(required = false) String trangThai,
                                        Model model, HttpSession session) {
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null) return "nhanvien/login";

        String maNDT = nhaDauTu.getMaNDT();
        List<LenhDat> danhSach;

        if (trangThai == null || trangThai.isBlank()) {
            danhSach = lenhDatService.timTheoMaNhaDauTu(maNDT);
        } else {
            danhSach = lenhDatService.timTheoMaNhaDauTuVaTrangThai(maNDT, trangThai);
            model.addAttribute("selectedTrangThai", trangThai);
        }

        Map<Long, String> giaFormattedMap = new HashMap<>();
        for (LenhDat lenh : danhSach) {
            giaFormattedMap.put(lenh.getMaGD(), formatGia(lenh.getGia()));
        }

        model.addAttribute("giaFormattedMap", giaFormattedMap);
        model.addAttribute("lenhDatList", danhSach);
        model.addAttribute("nhaDauTu", nhaDauTu);
        return "ndt/sao_ke_gdck";
    }

    @PostMapping("/nhadautu/huy-lenh")
    @ResponseBody
    public ResponseEntity<String> huyLenh(@RequestParam Long maGD) {
        Optional<LenhDat> lenhOpt = lenhDatService.findById(maGD);
        if (lenhOpt.isPresent()) {
            LenhDat lenh = lenhOpt.get();
            if ("Chờ".equalsIgnoreCase(lenh.getTrangThai())) {
                lenh.setTrangThai("Hủy");
                lenhDatService.save(lenh);
                return ResponseEntity.ok("Đã hủy");
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể hủy");
    }

    private String formatGia(Double gia) {
        return gia != null ? new DecimalFormat("#,###").format(gia) + " VND" : "Chưa cập nhật";
    }
}
