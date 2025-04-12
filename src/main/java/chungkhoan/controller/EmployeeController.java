package chungkhoan.controller;

import java.util.List;

import chungkhoan.service.NDTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.NhanVien;
import chungkhoan.repository.NhanVienRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EmployeeController {

	@Autowired NhanVienRepository nhanVienRepository;
	@Autowired NDTService ndtService;

	@GetMapping("/employees")
	public String employeeList(Model model) {
		List<NhanVien> list = nhanVienRepository.findAll();
		model.addAttribute("employees", list);
		model.addAttribute("employee", new NhanVien());

		if (list.isEmpty()) {
			model.addAttribute("noDataMessage", "Không có dữ liệu nhân viên.");
		}

		return "nhanvien/employee_list";
	}

	@PostMapping("/investors/undo")
	public String undoLastAction(RedirectAttributes redirectAttributes) {
		boolean success = ndtService.undoThaoTacCuoi();
		if (success) {
			redirectAttributes.addFlashAttribute("message", "Hoàn tác thành công");
			redirectAttributes.addFlashAttribute("messageType", "success");
		} else {
			redirectAttributes.addFlashAttribute("message", "Không có thao tác để hoàn tác");
			redirectAttributes.addFlashAttribute("messageType", "error");
		}
		return "redirect:/investors";
	}
}