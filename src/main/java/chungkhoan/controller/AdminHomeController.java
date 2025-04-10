package chungkhoan.controller;
//
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/nhanvien")
public class AdminHomeController {

	// Thoát về trang chủ
	@GetMapping("")
	public String home() {
		return "nhanvien/layout";
	}

	@GetMapping("/layout")
	public String showLayout() {
		return "nhanvien/layout";
	}
}