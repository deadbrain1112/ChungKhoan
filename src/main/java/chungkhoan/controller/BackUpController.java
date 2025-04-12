package chungkhoan.controller;

import chungkhoan.service.BackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
public class BackUpController {

	@Autowired
	private BackupService backupService;

	@GetMapping("/backup")
	public String backUpForm(@RequestParam(required = false) String dbName, Model model) {
		List<String> databases = backupService.getAllDatabases();
		model.addAttribute("databases", databases);

		if (dbName != null) {
			List<Map<String, Object>> backupList = backupService.getBackupHistory(dbName);
			model.addAttribute("backupList", backupList);
			model.addAttribute("selectedDb", dbName);
		}

		return "nhanvien/backup";
	}

	@PostMapping("/backup/device-create")
	public String createDevice(@RequestParam("dbName") String dbName, RedirectAttributes ra) {
		backupService.createBackupDevice(dbName);
		ra.addFlashAttribute("msg", "Đã tạo device cho " + dbName);
		return "redirect:/backup?dbName=" + dbName;
	}

	@PostMapping("/backup/save")
	public String backupDatabase(@RequestParam("dbName") String dbName,
								 @RequestParam(value = "deleteOld", required = false, defaultValue = "false") boolean deleteOld,
								 RedirectAttributes ra) {
		backupService.backupDatabase(dbName, deleteOld);
		ra.addFlashAttribute("msg", "Đã sao lưu " + dbName + " thành công.");
		return "redirect:/backup?dbName=" + dbName;
	}

	@PostMapping("/backup/recovery")
	public String restoreDatabase(@RequestParam("dbName") String dbName,
								  @RequestParam(value = "enableTimeRecovery", required = false) boolean enableTimeRecovery,
								  @RequestParam(value = "recoveryDate", required = false)
								  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recoveryDate,
								  @RequestParam(value = "recoveryTime", required = false)
								  @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime recoveryTime,
								  RedirectAttributes ra) {

		if (enableTimeRecovery && recoveryDate != null && recoveryTime != null) {
			backupService.restoreToTime(dbName, recoveryDate, recoveryTime);
			ra.addFlashAttribute("msg", "Đã phục hồi " + dbName + " về thời điểm " + recoveryDate + " " + recoveryTime);
		} else {
			backupService.restoreDatabase(dbName);
			ra.addFlashAttribute("msg", "Đã phục hồi " + dbName + " thành công.");
		}

		return "redirect:/backup?dbName=" + dbName;
	}
}
