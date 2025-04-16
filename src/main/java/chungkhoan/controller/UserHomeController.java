package chungkhoan.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.SoHuu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.service.CoPhieuService;
import chungkhoan.service.LichSuGiaService;
import chungkhoan.service.NDTService;
import chungkhoan.service.SoHuuService;
import chungkhoan.service.TaiKhoanNganHangService;

@Controller
public class UserHomeController {

    @Autowired
    private NDTService ndtService;

    @Autowired
    private TaiKhoanNganHangService taiKhoanNganHangService;

    @Autowired
    private CoPhieuService coPhieuService;

    @Autowired
    private LichSuGiaService lichSuGiaService;

    @Autowired
    private SoHuuService soHuuService;

    @GetMapping("/nhadautu/home")
    public String home(Model model) {
        // Tạm hardcode username (trong thực tế lấy từ session hoặc Spring Security)
        String username = "NDT01";

        // Lấy thông tin nhà đầu tư
        NhaDauTu nhaDauTu;
        try {
            nhaDauTu = ndtService.getNhaDauTuByUsername(username);
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: Không thể tìm thấy nhà đầu tư duy nhất với username: " + username);
            return "ndt/home";
        }

        // Lấy danh sách tài khoản ngân hàng của nhà đầu tư
        List<TaiKhoanNganHang> danhSachTaiKhoan = taiKhoanNganHangService.getAllByNDT(nhaDauTu);

        // Kiểm tra xem danh sách tài khoản có trống hay không và tạo đối tượng mặc định
        TaiKhoanNganHang taiKhoan = danhSachTaiKhoan.isEmpty() ? new TaiKhoanNganHang() : danhSachTaiKhoan.get(0);

        // Kiểm tra taiKhoan và soTien trong controller
        if (taiKhoan == null || taiKhoan.getSoTien() == null) {
            taiKhoan.setSoTien(BigDecimal.ZERO); // Gán giá trị mặc định cho số tiền
        }

        // Lấy danh sách mã cổ phiếu mà nhà đầu tư sở hữu từ SoHuu
        List<String> danhSachMaCP = soHuuService.getMaCPByNDT(nhaDauTu.getMaNDT());

        // Lấy danh sách sở hữu của nhà đầu tư
        List<SoHuu> danhSachSoHuu = soHuuService.getSoHuuByNDT(nhaDauTu.getMaNDT());

        // Lấy thông tin cổ phiếu tương ứng với danh sách mã cổ phiếu
        List<CoPhieu> danhSachCP = coPhieuService.findByMaCPIn(danhSachMaCP);

        if (danhSachSoHuu.isEmpty()) {
            model.addAttribute("nullCP", "Không có dữ liệu cổ phiếu!");
        }

        // Định dạng số với dấu phẩy phân cách hàng nghìn
        DecimalFormat decimalFormat = new DecimalFormat("#,###");

        // Định dạng số dư tài khoản
        String formattedSoTien = taiKhoan.getSoTien() != null 
                ? decimalFormat.format(taiKhoan.getSoTien()) 
                : "0";

        // Tính giá trị thị trường và tổng giá trị của từng cổ phiếu
        Map<String, String> giaThiTruongMap = new HashMap<>(); // Đổi kiểu từ Float sang String để lưu giá trị đã định dạng
        Map<String, String> tongGiaTriMap = new HashMap<>();
        Map<String, Integer> soLuongMap = new HashMap<>(); // Lưu số lượng cổ phiếu sở hữu

        // Lưu số lượng từ SoHuu vào map để sử dụng
        for (SoHuu soHuu : danhSachSoHuu) {
            soLuongMap.put(soHuu.getMaCP(), soHuu.getSoLuong());
        }

        for (CoPhieu cp : danhSachCP) {
            // Lấy giá thị trường (giaTC) của cổ phiếu từ LichSuGia
            Float giaTC = lichSuGiaService.getGiaThamChieuMoiNhat(cp.getMaCP());
            
            // Định dạng giá thị trường với dấu phẩy
            String formattedGiaTC = giaTC != null ? decimalFormat.format(giaTC) : "0";
            giaThiTruongMap.put(cp.getMaCP(), formattedGiaTC);

            // Lấy số lượng từ SoHuu
            Integer soLuong = soLuongMap.getOrDefault(cp.getMaCP(), 0);

            // Tính tổng giá trị = Số lượng sở hữu * Giá trị thị trường
            BigDecimal tongGiaTri = BigDecimal.valueOf(soLuong)
                    .multiply(BigDecimal.valueOf(giaTC != null ? giaTC : 0));
            tongGiaTriMap.put(cp.getMaCP(), decimalFormat.format(tongGiaTri));
        }

        // Đẩy dữ liệu lên giao diện
        model.addAttribute("nhaDauTu", nhaDauTu);
        model.addAttribute("taiKhoan", taiKhoan);
        model.addAttribute("danhSachTaiKhoan", danhSachTaiKhoan);
        model.addAttribute("danhSachCoPhieu", danhSachSoHuu);
        model.addAttribute("formattedSoTien", formattedSoTien);
        model.addAttribute("giaThiTruongMap", giaThiTruongMap);
        model.addAttribute("tongGiaTriMap", tongGiaTriMap);

        return "ndt/home";
    }
}