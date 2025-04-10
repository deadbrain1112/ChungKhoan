package chungkhoan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SignInController {
	@GetMapping("/sign-in-request")
	public String signInForm() {
		return "ndt/sign_in_request";
	}
	
//	@PostMapping("/sign-in-request")
}