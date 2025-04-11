package chungkhoan.controller;

import chungkhoan.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class BackUpController {

	@Autowired
	private DatabaseService databaseService;

	@GetMapping("/backup")
	public String backUpForm(Model model) {
		List<String> databases = databaseService.getDatabases();
		model.addAttribute("databases", databases);

		return "nhanvien/backup";
	}
}
