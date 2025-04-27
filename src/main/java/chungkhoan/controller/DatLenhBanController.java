package chungkhoan.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.LenhDat;
import chungkhoan.entity.LichSuGia;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.service.CoPhieuService;
import chungkhoan.service.LenhDatService;
import chungkhoan.service.LichSuGiaService;
import chungkhoan.service.SoHuuService;
import chungkhoan.service.TaiKhoanNganHangService;
import jakarta.servlet.http.HttpSession;

@Controller
public class DatLenhBanController {

    @Autowired
    private TaiKhoanNganHangService taiKhoanNganHangService;

    @Autowired
    private CoPhieuService coPhieuService;
    
    @Autowired
    private LichSuGiaService lichSuGiaService;
    
    @Autowired
    private LenhDatService lenhDatService;
    
    @Autowired
    private SoHuuService soHuuService;

    @GetMapping("/nhadautu/dat-lenh-ban")
    public String getView(@RequestParam(value = "maCP", required = false) String maCP, Model model, HttpSession session) {
        // Lấy thông tin nhà đầu tư từ session
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");

        if (nhaDauTu == null) {
            return "nhanvien/login";
        }

        // Lấy danh sách tài khoản ngân hàng của nhà đầu tư
        List<TaiKhoanNganHang> danhSachTaiKhoan = taiKhoanNganHangService.getAllByNDT(nhaDauTu);
        TaiKhoanNganHang taiKhoan = danhSachTaiKhoan.isEmpty() ? null : danhSachTaiKhoan.get(0);

        // Định dạng số dư tiền
        String formattedSoTien = "0";
        if (taiKhoan != null && taiKhoan.getSoTien() != null) {
            DecimalFormat decimalFormat = new DecimalFormat("#.###");
            formattedSoTien = decimalFormat.format(taiKhoan.getSoTien());
        }

        model.addAttribute("danhSachTaiKhoan", danhSachTaiKhoan);
        model.addAttribute("taiKhoan", taiKhoan);
        model.addAttribute("formattedSoTien", formattedSoTien);

        if (maCP != null && !maCP.trim().isEmpty()) {
            // Gọi service để lấy giá cổ phiếu từ stored procedure
            LichSuGia lichSuGia = lichSuGiaService.layGiaMoiNhat(maCP.trim());

            if (lichSuGia != null) {
                model.addAttribute("coPhieu", lichSuGia);
            } else {
                model.addAttribute("khongTimThay", true);
            }
        }

        return "ndt/dat_lenh_ban";
    }
    
    @PostMapping("/nhadautu/dat-lenh-ban")
    public String datLenhBan(@RequestParam("maCP") String maCP,
                             @RequestParam("nganHang") String maNH,
                             @RequestParam("loaiLenh") String loaiLenh,
                             @RequestParam("soLuong") Integer soLuong,
                             @RequestParam("gia") double gia,
                             @RequestParam("matKhau") String matKhau,
                             Model model,
                             HttpSession session) {

        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");

        List<TaiKhoanNganHang> danhSachTaiKhoan = taiKhoanNganHangService.getAllByNDT(nhaDauTu);
        model.addAttribute("danhSachTaiKhoan", danhSachTaiKhoan);

        TaiKhoanNganHang taiKhoan = danhSachTaiKhoan.isEmpty() ? null : danhSachTaiKhoan.get(0);
        
        // Định dạng số dư tiền
        String formattedSoTien = "0";
        double soTien = 0;
        if (taiKhoan != null && taiKhoan.getSoTien() != null) {
            soTien = taiKhoan.getSoTien().doubleValue();  // Chuyển BigDecimal thành double
            DecimalFormat decimalFormat = new DecimalFormat("#.###");
            formattedSoTien = decimalFormat.format(soTien);
        }
        
        model.addAttribute("formattedSoTien", formattedSoTien);

        Optional<CoPhieu> optCoPhieu = coPhieuService.findById(maCP);
        if (optCoPhieu.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy cổ phiếu");
            return "ndt/dat_lenh_ban";
        }

        CoPhieu coPhieu = optCoPhieu.get();

        // Lấy giá sàn mới nhất của cổ phiếu từ bảng LichSuGia
        LichSuGia lichSuGia = lichSuGiaService.layGiaMoiNhat(maCP);
        if (lichSuGia == null) {
            model.addAttribute("error", "Không có dữ liệu giá sàn cho cổ phiếu này");
            return "ndt/dat_lenh_ban";
        }

        // Chuyển giá sàn thành double
        double giaSan = lichSuGia.getGiaSan();  // Giá sàn là kiểu double

        // Kiểm tra nếu là lệnh LO, đảm bảo giá bán không thấp hơn giá sàn
        if ("LO".equals(loaiLenh)) {
            if (gia < giaSan) {  // So sánh giá bán và giá sàn kiểu double
                model.addAttribute("error", "Giá bán không được thấp hơn giá sàn!");
                return "ndt/dat_lenh_ban";
            }
        }

        // Kiểm tra xem nhà đầu tư có đủ cổ phiếu để bán hay không
        if (soHuuService.getSoLuong(nhaDauTu.getMaNDT(), maCP) < soLuong) {
            model.addAttribute("error", "Số lượng cổ phiếu không đủ để bán!");
            return "ndt/dat_lenh_ban";
        }

        // Tạo đối tượng lệnh đặt
        LenhDat lenhDat = LenhDat.builder()
                    .coPhieu(coPhieu)
                    .taiKhoanNganHang(taiKhoan)
                    .loaiGD("B")  // Loại giao dịch "B" cho bán
                    .loaiLenh(loaiLenh)
                    .soLuong(soLuong)
                    .gia(gia)  // Truyền giá vào là kiểu double
                    .trangThai("Chờ")  // Trạng thái ban đầu là "Chờ"
                    .ngayGD(LocalDateTime.now())
                    .build();

        // Lưu lệnh đặt
        lenhDatService.save(lenhDat);

        model.addAttribute("success", "Đặt lệnh bán thành công, chờ khớp lệnh!");

        return "redirect:/nhadautu/dat-lenh-ban";
    }

}
