package chungkhoan.controller;

import chungkhoan.service.NDTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.NhanVien;
import chungkhoan.repository.NDTRepository;
import chungkhoan.repository.NhanVienRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;

@Controller
public class SignInController {
    @Autowired private NDTService ndtService;

    @GetMapping("/sign-in-request")
	public String signInForm() {
		return "ndt/sign_in_request";
	}

    @PostMapping("/sign-in-request")
    public String handleSignIn(HttpServletRequest request) {
        String hoTen = request.getParameter("hoTen");
        LocalDate ngaySinh = LocalDate.parse(request.getParameter("ngaySinh"));
        String diaChi = request.getParameter("diaChi");
        String phone = request.getParameter("phone");
        String cmnd = request.getParameter("cmnd");
        String gioiTinh = request.getParameter("gioiTinh");
        String email = request.getParameter("email");

        NhaDauTu ndt = new NhaDauTu();
        ndt.setHoTen(hoTen);
        ndt.setNgaySinh(ngaySinh);
        ndt.setDiaChi(diaChi);
        ndt.setPhone(phone);
        ndt.setCmnd(cmnd);
        ndt.setGioiTinh(gioiTinh);
        ndt.setEmail(email);
        ndt.setMkGiaoDich("1");


        ndtService.themNhaDauTuBangSP(ndt);

        return "redirect:/sign-in-request?success";
    }
}