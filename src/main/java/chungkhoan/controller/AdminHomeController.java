package chungkhoan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/nhanvien")
public class AdminHomeController {
	@GetMapping("/layout")
	public String showLayout() {
		return "nhanvien/layout";
	}
}

