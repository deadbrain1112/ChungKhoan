package chungkhoan.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.LenhDat;
import chungkhoan.entity.LichSuGia;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.service.CoPhieuService;
import chungkhoan.service.LenhDatService;
import chungkhoan.service.LichSuGiaService;
import chungkhoan.service.TaiKhoanNganHangService;
import jakarta.servlet.http.HttpSession;

@Controller
public class DatLenhMuaController {

    @Autowired
    private TaiKhoanNganHangService taiKhoanNganHangService;

    @Autowired
    private CoPhieuService coPhieuService;

    @Autowired
    private LichSuGiaService lichSuGiaService;

    @Autowired
    private LenhDatService lenhDatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/nhadautu/dat-lenh-mua")
    public String getView(@RequestParam(value = "maCP", required = false) String maCP,
    					  @RequestParam(value = "nganHang", required = false) String nganHang,
                          Model model,
                          HttpSession session) {
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null) return "nhanvien/login";

        List<TaiKhoanNganHang> danhSachTaiKhoan = taiKhoanNganHangService.getAllByNDT(nhaDauTu);
        TaiKhoanNganHang taiKhoan = null;

        if (nganHang != null && !nganHang.isBlank()) {
            for (TaiKhoanNganHang tk : danhSachTaiKhoan) {
                if (tk.getNganHang().getMaNH().equals(nganHang)) {
                    taiKhoan = tk;
                    break;
                }
            }
        }

        if (taiKhoan == null && !danhSachTaiKhoan.isEmpty()) {
            taiKhoan = danhSachTaiKhoan.get(0);
        }

        double soTien = (taiKhoan != null && taiKhoan.getSoTien() != null) ? taiKhoan.getSoTien().doubleValue() : 0;
        String formattedSoTien = new DecimalFormat("#,###").format(soTien) + " VND";

        model.addAttribute("nhaDauTu", nhaDauTu);
        model.addAttribute("danhSachTaiKhoan", danhSachTaiKhoan);
        model.addAttribute("taiKhoan", taiKhoan);
        model.addAttribute("formattedSoTien", formattedSoTien);

        if (maCP != null && !maCP.isBlank()) {
            LichSuGia gia = lichSuGiaService.layGiaMoiNhat(maCP.trim());
            if (gia != null) {
            	model.addAttribute("lichSuGia", gia);
                model.addAttribute("giaThamChieu", formatGia(gia.getGiaTC()));
                model.addAttribute("giaTran", formatGia(gia.getGiaTran()));
                model.addAttribute("giaSan", formatGia(gia.getGiaSan()));
            } else {
                model.addAttribute("khongTimThay", true);
            }
        }
        
        model.addAttribute("tatCaCoPhieu", coPhieuService.getAllCoPhieu());
        
        return "ndt/dat_lenh_mua";
    }

    @PostMapping("/nhadautu/dat-lenh-mua")
    public String datLenhMua(@RequestParam String maCP,
                             @RequestParam String nganHang,
                             @RequestParam String loaiLenh,
                             @RequestParam Integer soLuong,
                             @RequestParam(required = false) Double gia,
                             @RequestParam String matKhau,
                             Model model,
                             HttpSession session) {

        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null) return "nhanvien/login";

        List<TaiKhoanNganHang> danhSachTaiKhoan = taiKhoanNganHangService.getAllByNDT(nhaDauTu);
        TaiKhoanNganHang taiKhoan = null;

        if (nganHang != null && !nganHang.isBlank()) {
            for (TaiKhoanNganHang tk : danhSachTaiKhoan) {
                if (tk.getNganHang().getMaNH().equals(nganHang)) {
                    taiKhoan = tk;
                    break;
                }
            }
        }

        if (taiKhoan == null && !danhSachTaiKhoan.isEmpty()) {
            taiKhoan = danhSachTaiKhoan.get(0);
        }

        model.addAttribute("nhaDauTu", nhaDauTu);
        model.addAttribute("danhSachTaiKhoan", danhSachTaiKhoan);
        model.addAttribute("taiKhoan", taiKhoan);

        double soTien = (taiKhoan != null && taiKhoan.getSoTien() != null) ? taiKhoan.getSoTien().doubleValue() : 0;
        model.addAttribute("formattedSoTien", new DecimalFormat("#,###").format(soTien) + " VND");

        Optional<CoPhieu> coPhieuOpt = coPhieuService.findById(maCP);
        if (coPhieuOpt.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy cổ phiếu");
            return "ndt/dat_lenh_mua";
        }

        if (soLuong == null || soLuong <= 0) {
            model.addAttribute("error", "Số lượng đặt lệnh phải lớn hơn 0!");
            return "ndt/dat_lenh_mua";
        }

        if (matKhau == null || !matKhau.equals(nhaDauTu.getMkGiaoDich())) {
            model.addAttribute("error", "Mật khẩu giao dịch không đúng!");
            return "ndt/dat_lenh_mua";
        }

        LichSuGia lichSuGia = lichSuGiaService.layGiaMoiNhat(maCP);
        if (lichSuGia == null) {
            model.addAttribute("error", "Không có dữ liệu giá sàn cho cổ phiếu này");
            return "ndt/dat_lenh_mua";
        }

        double giaDat = 0;

        if ("LO".equalsIgnoreCase(loaiLenh)) {
            if (gia == null) {
                model.addAttribute("error", "Thiếu dữ liệu giá mua!");
                return "ndt/dat_lenh_mua";
            }

            Double giaTran = lichSuGia.getGiaTran();
            Double giaSan = lichSuGia.getGiaSan();

            if (giaTran == null || giaSan == null) {
                model.addAttribute("error", "Thiếu thông tin giá trần hoặc sàn!");
                return "ndt/dat_lenh_mua";
            }

            if (gia < giaSan || gia > giaTran) {
                model.addAttribute("error", "Giá mua không được thấp hơn giá sàn hoặc lớn hơn giá trần!");
                return "ndt/dat_lenh_mua";
            }

            giaDat = gia;
            double tongTien = giaDat * soLuong;
            if (soTien < tongTien) {
                model.addAttribute("error", "Số dư không đủ để đặt lệnh mua!");
                return "ndt/dat_lenh_mua";
            }

        } else if ("ATO".equalsIgnoreCase(loaiLenh) || "ATC".equalsIgnoreCase(loaiLenh)) {
            Double giaTran = lichSuGia.getGiaTran();
            double maxGia = (giaTran != null) ? giaTran : 0.0;

            double tongTien = maxGia * soLuong;
            if (soTien < tongTien) {
                model.addAttribute("error", "Số dư không đủ để đặt lệnh " + loaiLenh + " theo giá trần!");
                return "ndt/dat_lenh_mua";
            }

            giaDat = 0;
        } else {
            model.addAttribute("error", "Loại lệnh không hợp lệ!");
            return "ndt/dat_lenh_mua";
        }

       LenhDat lenh = LenhDat.builder()
               .coPhieu(coPhieuOpt.get())
               .taiKhoanNganHang(taiKhoan)
               .loaiGD("M")
               .loaiLenh(loaiLenh)
               .soLuong(soLuong)
               .gia(giaDat)
               .trangThai("Chờ")
               .ngayGD(LocalDateTime.now())
               .build();

        lenhDatService.save(lenh);

        messagingTemplate.convertAndSend("/topic/stock-board", createOrderMessage(maCP, gia, soLuong, "M"));
        model.addAttribute("success", "Đặt lệnh mua thành công!");

        model.addAttribute("tatCaCoPhieu", coPhieuService.getAllCoPhieu());

        return "redirect:/nhadautu/dat-lenh-mua";
    }

    private Object createOrderMessage (String maCPInput,double giaInput, int soLuongInput, String loaiGDInput){
        return new Object() {
            public String maCP = maCPInput;
            public double gia = giaInput;
            public int soLuong = soLuongInput;
            public String loaiGD = loaiGDInput;
        };
    }

    private String formatGia(Double gia) {
        return gia != null ? new DecimalFormat("#,###").format(gia) + " VND" : "Chưa cập nhật";
    }
    
    @GetMapping("/nhadautu/so-du")
    @ResponseBody
    public String laySoDuTheoMaNH(@RequestParam("nganHang") String maNH, HttpSession session) {
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null) return "0";

        List<TaiKhoanNganHang> danhSach = taiKhoanNganHangService.getAllByNDT(nhaDauTu);
        for (TaiKhoanNganHang tk : danhSach) {
            if (tk.getNganHang().getMaNH().equals(maNH)) {
                BigDecimal soTien = tk.getSoTien() != null ? tk.getSoTien() : BigDecimal.ZERO;
                return new DecimalFormat("#,###").format(soTien);
            }
        }

        return "0";
    }
    
    @GetMapping("/nhadautu/gia-co-phieu")
    @ResponseBody
    public ResponseEntity<?> layGiaCoPhieu(@RequestParam("maCP") String maCP) {
        LichSuGia gia = lichSuGiaService.layGiaMoiNhat(maCP.trim());

        if (gia == null) {
            return ResponseEntity.notFound().build();
        }

        DecimalFormat df = new DecimalFormat("#,###");
        Map<String, String> result = new HashMap<>();
        result.put("maCP", gia.getMaCP());
        result.put("giaTC", df.format(gia.getGiaTC()) + " VND");
        result.put("giaTran", df.format(gia.getGiaTran()) + " VND");
        result.put("giaSan", df.format(gia.getGiaSan()) + " VND");

        return ResponseEntity.ok(result);
    }
}

