package chungkhoan.controller;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.service.NDTService;
import chungkhoan.service.TaiKhoanNganHangService;
import chungkhoan.service.CoPhieuService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class UserHomeController {

    @Autowired
    private NDTService ndtService;

    @Autowired
    private TaiKhoanNganHangService taiKhoanNganHangService;

    @Autowired
    private CoPhieuService coPhieuService;

    @GetMapping("/nhadautu/home")
    public String home(HttpSession session, Model model) {
        // Lấy username từ session
        String username = (String) session.getAttribute("username");
        String password = (String) session.getAttribute("password");

        // Kiểm tra xem người dùng đã đăng nhập chưa
        if (username == null || password == null) {
            return "redirect:/login?error=unauthorized";
        }

        // Lấy thông tin NDT từ username
        var nhaDauTu = ndtService.getNhaDauTuByUsername(username);
        if (nhaDauTu == null) {
            session.invalidate(); // Xóa session nếu không tìm thấy
            return "redirect:/login?error=no_ndt_found";
        }

        // Lấy thông tin tài khoản ngân hàng của NDT
        TaiKhoanNganHang taiKhoan = taiKhoanNganHangService.getTaiKhoanByNDT(nhaDauTu);
        if (taiKhoan == null) {
            model.addAttribute("nullTKNH", "Không tìm thấy thông tin tài khoản!");
        }

        // Lấy danh sách cổ phiếu thuộc NDT
        List<CoPhieu> danhSachCoPhieu = coPhieuService.getAllCoPhieu();
        if (danhSachCoPhieu.isEmpty()) {
        	model.addAttribute("nullCP", "Không có dữ liệu cổ phiếu!");
        }

        // Đưa dữ liệu vào model để hiển thị trên giao diện
        model.addAttribute("taiKhoan", taiKhoan);
        model.addAttribute("danhSachCoPhieu", danhSachCoPhieu);
        model.addAttribute("nhaDauTu", nhaDauTu);

        return "ndt/home";
    }
}
