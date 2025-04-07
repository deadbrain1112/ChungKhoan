//package chungkhoan.controller;
//
//import jakarta.servlet.http.HttpSession;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.GetMapping;
//
//@Controller
//public class HomeController {
//
//    @GetMapping("/ndt/success")
//    public String homeNDT(HttpSession session) {
//        Role role = (Role) session.getAttribute("role");
//        if (role == Role.ROLE_INVESTOR) {
//            return "ndt/success";
//        }
//        return "redirect:/login?error=unauthorized";
//    }
//
//    @GetMapping("/nhanvien/success")
//    public String homeNhanVien(HttpSession session) {
//        Role role = (Role) session.getAttribute("role");
//        if (role == Role.ROLE_EMPLOYEE) {
//            return "nhanvien/success";
//        }
//        return "redirect:/login?error=unauthorized";
//    }
//
//    // Thoát về trang chủ
//    @GetMapping("/")
//    public String home() {
//        return "redirect:/layout";
//    }
//}
