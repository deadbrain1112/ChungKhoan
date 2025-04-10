package chungkhoan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserHomeController {
	
	// Hiện trang tài khoản ndt
	@GetMapping("/home")
	public String home() {
		return "ndt/home";
	}
	
//	@GetMapping("/exit")
//	public String exit() {
//		
//	}
}
