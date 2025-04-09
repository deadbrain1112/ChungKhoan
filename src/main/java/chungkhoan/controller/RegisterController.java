package chungkhoan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/register")
public class RegisterController {
	@GetMapping("")
	public String showRegister() {
		return "nhanvien/register";
	}
}