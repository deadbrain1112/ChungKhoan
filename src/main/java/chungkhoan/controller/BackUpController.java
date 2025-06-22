package chungkhoan.controller;

import chungkhoan.service.BackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
public class BackUpController {

	@Autowired
	private BackupService backupService;
	String dbName = "QUANLYGIAODICHCHUNGKHOAN";

	@GetMapping("/backup")
	public String backUpForm(Model model) {
		String dbName = "QUANLYGIAODICHCHUNGKHOAN";
		String deviceName = "QUANLYGIAODICHCHUNGKHOAN";

		List<Map<String, Object>> backupList = backupService.getBackupHistory(dbName, deviceName);

		model.addAttribute("backupList", backupList);
		model.addAttribute("selectedDb", dbName);

		return "nhanvien/backup";
	}

	@PostMapping("/backup/device-create")
	public String createDevice( RedirectAttributes ra) {
		backupService.createBackupDevice(dbName);
		ra.addFlashAttribute("msg", "Đã tạo device cho " + dbName);
		return "redirect:/backup?dbName=" + dbName;
	}

	@PostMapping("/backup/save")
	public String backupDatabase(@RequestParam(value = "deleteOld", required = false, defaultValue = "false") boolean deleteOld,
								 RedirectAttributes ra) {
		backupService.backupDatabase(dbName, deleteOld);
		ra.addFlashAttribute("msg", "Đã sao lưu " + dbName + " thành công.");
		return "redirect:/backup?dbName=" + dbName;
	}

	@PostMapping("/backup/recovery")
	public String restoreSelectedBackup(
			@RequestParam("selectedBackup") String selectedBackup,
			RedirectAttributes ra) {
		try {
			System.out.println(selectedBackup);
			String[] parts = selectedBackup.split("::");
			String path = parts[0];

			int index = Integer.parseInt(parts[1]);

			backupService.restoreBackup(path, index);

			ra.addFlashAttribute("msg", "Phục hồi cơ sở dữ liệu thành công.");
		} catch (Exception e) {
			ra.addFlashAttribute("errorMsg", "Phục hồi thất bại: " + e.getMessage());
		}
		return "redirect:/backup";
	}


}