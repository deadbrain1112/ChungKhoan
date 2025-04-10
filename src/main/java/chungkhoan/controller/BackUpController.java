package chungkhoan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BackUpController {
	@GetMapping("/backup")
	public String backUpForm() {
		return "nhanvien/backup";
	}
}
