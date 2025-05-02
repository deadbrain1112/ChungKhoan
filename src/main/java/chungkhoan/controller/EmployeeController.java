package chungkhoan.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import chungkhoan.entity.NhanVien;
import chungkhoan.service.NhanVienService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class EmployeeController {

	@Autowired
	private NhanVienService nhanVienService;

	@GetMapping("/employees")
	public String employeeList(@RequestParam(defaultValue = "0") int page,
							   @RequestParam(defaultValue = "5") int size,
							   Model model,
							   HttpSession session) {
		// Lấy danh sách nhân viên chính từ cơ sở dữ liệu
		Page<NhanVien> employeePage = nhanVienService.getPaginated(0, Integer.MAX_VALUE);

		// Lấy danh sách nhân viên tạm từ session
		@SuppressWarnings("unchecked")
		List<NhanVien> tempList = (List<NhanVien>) session.getAttribute("temporaryEmployees");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		// Kết hợp danh sách: danh sách chính trước, danh sách tạm sau
		List<NhanVien> allEmployees = new ArrayList<>();
		allEmployees.addAll(employeePage.getContent());
		allEmployees.addAll(tempList);

		// Tính toán phân trang cho danh sách kết hợp
		int totalItems = allEmployees.size();
		int start = page * size;
		int end = Math.min(start + size, totalItems);

		// Đảm bảo start và end hợp lệ
		if (start >= totalItems && totalItems > 0) {
			page = (totalItems - 1) / size;
			start = page * size;
			end = Math.min(start + size, totalItems);
		} else if (start >= totalItems) {
			start = 0;
			end = 0;
		}

		List<NhanVien> allEmployeesPageContent = (start < end) ? allEmployees.subList(start, end) : new ArrayList<>();

		// Tạo đối tượng Page cho danh sách kết hợp
		Page<NhanVien> allEmployeesPage = new PageImpl<>(allEmployeesPageContent, PageRequest.of(page, size), totalItems);

		// Thêm vào model
		model.addAttribute("employees", allEmployeesPage);
		model.addAttribute("temporaryEmployees", tempList);
		if (allEmployeesPage.isEmpty()) {
			model.addAttribute("message", "Không có dữ liệu nhân viên.");
			model.addAttribute("messageType", "danger");
		}

		// Kiểm tra nếu có thể hoàn tác
		model.addAttribute("canUndo", !nhanVienService.isUndoStackEmpty());

		// Thêm logging để kiểm tra
		System.out.println("TempList size: " + tempList.size());
		System.out.println("AllEmployees size: " + allEmployees.size());
		System.out.println("Page content size: " + allEmployeesPageContent.size());

		return "nhanvien/employee_list";
	}

	@PostMapping("/employees/add-temp")
	public String addTempEmployee(@ModelAttribute NhanVien employee,
								  @RequestParam(defaultValue = "0") int page,
								  @RequestParam(defaultValue = "5") int size,
								  HttpSession session,
								  RedirectAttributes redirectAttributes) {
		// Kiểm tra ngày sinh
		LocalDate ngaySinh = employee.getNgaySinh();
		LocalDate currentDate = LocalDate.now();
		if (ngaySinh == null || ngaySinh.isAfter(currentDate) || ngaySinh.getYear() < 1900) {
			redirectAttributes.addFlashAttribute("message", "Ngày sinh không hợp lệ!");
			redirectAttributes.addFlashAttribute("messageType", "danger");
			return "redirect:/employees?page=" + page + "&size=" + size;
		}

		@SuppressWarnings("unchecked")
		List<NhanVien> tempList = (List<NhanVien>) session.getAttribute("temporaryEmployees");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		// Kiểm tra trùng MaNV
		if (nhanVienService.existsById(employee.getMaNV()) || tempList.stream().anyMatch(emp -> emp.getMaNV().equals(employee.getMaNV()))) {
			redirectAttributes.addFlashAttribute("message", "Mã nhân viên " + employee.getMaNV() + " đã tồn tại!");
			redirectAttributes.addFlashAttribute("messageType", "error");
			return "redirect:/employees?page=" + page + "&size=" + size;
		}

		// Kiểm tra trùng CMND
		if (nhanVienService.existsByCmnd(employee.getCmnd()) || tempList.stream().anyMatch(emp -> emp.getCmnd().equals(employee.getCmnd()))) {
			redirectAttributes.addFlashAttribute("message", "CMND " + employee.getCmnd() + " đã tồn tại!");
			redirectAttributes.addFlashAttribute("messageType", "error");
			return "redirect:/employees?page=" + page + "&size=" + size;
		}

		// Thêm vào danh sách tạm
		tempList.add(employee);
		session.setAttribute("temporaryEmployees", tempList);
		redirectAttributes.addFlashAttribute("message", "Đã thêm tạm nhân viên " + employee.getMaNV());
		redirectAttributes.addFlashAttribute("messageType", "success");

		// Thêm logging để kiểm tra
		System.out.println("Added employee to tempList: " + employee.getMaNV());
		System.out.println("TempList after adding: " + tempList);

		return "redirect:/employees?page=" + page + "&size=" + size;
	}

	@PostMapping("/employees/edit-temp")
	public String editTempEmployee(@ModelAttribute NhanVien employee,
								   HttpSession session,
								   RedirectAttributes redirectAttributes) {
		// Kiểm tra ngày sinh
		LocalDate ngaySinh = employee.getNgaySinh();
		LocalDate currentDate = LocalDate.now();
		if (ngaySinh == null || ngaySinh.isAfter(currentDate) || ngaySinh.getYear() < 1900) {
			redirectAttributes.addFlashAttribute("message", "Ngày sinh không hợp lệ!");
			redirectAttributes.addFlashAttribute("messageType", "danger");
			return "redirect:/employees";
		}

		@SuppressWarnings("unchecked")
		List<NhanVien> tempList = (List<NhanVien>) session.getAttribute("temporaryEmployees");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		// Kiểm tra trùng CMND (ngoại trừ chính nhân viên đang sửa)
		NhanVien existingEmployee = nhanVienService.findById(employee.getMaNV()).orElse(null);
		if (existingEmployee != null && !existingEmployee.getCmnd().equals(employee.getCmnd())) {
			if (nhanVienService.existsByCmnd(employee.getCmnd()) || tempList.stream()
					.filter(emp -> !emp.getMaNV().equals(employee.getMaNV()))
					.anyMatch(emp -> emp.getCmnd().equals(employee.getCmnd()))) {
				redirectAttributes.addFlashAttribute("message", "CMND " + employee.getCmnd() + " đã tồn tại!");
				redirectAttributes.addFlashAttribute("messageType", "error");
				return "redirect:/employees";
			}
		}

		// Cập nhật hoặc thêm vào danh sách tạm
		boolean replaced = false;
		for (int i = 0; i < tempList.size(); i++) {
			if (tempList.get(i).getMaNV().equals(employee.getMaNV())) {
				tempList.set(i, employee);
				replaced = true;
				break;
			}
		}
		if (!replaced) {
			tempList.add(employee);
		}
		session.setAttribute("temporaryEmployees", tempList);
		redirectAttributes.addFlashAttribute("message", "Đã cập nhật tạm nhân viên " + employee.getMaNV());
		redirectAttributes.addFlashAttribute("messageType", "success");

		return "redirect:/employees";
	}

	@PostMapping("/employees/save-all")
	public String saveAllEmployees(HttpSession session, RedirectAttributes redirectAttributes) {
		@SuppressWarnings("unchecked")
		List<NhanVien> tempList = (List<NhanVien>) session.getAttribute("temporaryEmployees");
		if (tempList != null && !tempList.isEmpty()) {
			try {
				for (NhanVien employee : tempList) {
					Optional<NhanVien> existing = nhanVienService.findById(employee.getMaNV());
					if (existing.isPresent()) {
						nhanVienService.capNhatNhanVien(employee.getMaNV(), employee);
					} else {
						nhanVienService.themNhanVienBangSP(employee);
					}
				}
				session.removeAttribute("temporaryEmployees");
				redirectAttributes.addFlashAttribute("message", "Đã ghi vào cơ sở dữ liệu!");
				redirectAttributes.addFlashAttribute("messageType", "success");
			} catch (Exception e) {
				String errorMessage = e.getMessage();
				if (errorMessage.contains("Mã nhân viên đã tồn tại")) {
					redirectAttributes.addFlashAttribute("message", "Lỗi: Mã nhân viên đã tồn tại!");
				} else if (errorMessage.contains("CMND đã tồn tại")) {
					redirectAttributes.addFlashAttribute("message", "Lỗi: CMND đã tồn tại!");
				} else {
					redirectAttributes.addFlashAttribute("message", "Lỗi khi lưu nhân viên: " + errorMessage);
				}
				redirectAttributes.addFlashAttribute("messageType", "error");
			}
		} else {
			redirectAttributes.addFlashAttribute("message", "Không có nhân viên tạm để ghi.");
			redirectAttributes.addFlashAttribute("messageType", "error");
		}
		return "redirect:/employees";
	}

	@PostMapping("/employees/delete")
	public String deleteEmployee(@RequestParam String maNV, RedirectAttributes redirectAttributes) {
		try {
			nhanVienService.xoaNhanVien(maNV);
			redirectAttributes.addFlashAttribute("message", "Đã xóa nhân viên " + maNV);
			redirectAttributes.addFlashAttribute("messageType", "success");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("message", "Xóa thất bại");
			redirectAttributes.addFlashAttribute("messageType", "error");
		}
		return "redirect:/employees";
	}

	@PostMapping("/employees/remove-temp")
	public String removeTempEmployee(@RequestParam String maNV, HttpSession session, RedirectAttributes redirectAttributes) {
		@SuppressWarnings("unchecked")
		List<NhanVien> tempList = (List<NhanVien>) session.getAttribute("temporaryEmployees");
		if (tempList != null) {
			tempList.removeIf(emp -> emp.getMaNV().equals(maNV));
			session.setAttribute("temporaryEmployees", tempList);
			redirectAttributes.addFlashAttribute("message", "Đã xóa nhân viên tạm có mã " + maNV);
			redirectAttributes.addFlashAttribute("messageType", "success");
		} else {
			redirectAttributes.addFlashAttribute("message", "Không tìm thấy nhân viên tạm nào để xóa.");
			redirectAttributes.addFlashAttribute("messageType", "error");
		}
		return "redirect:/employees";
	}

	@GetMapping("/employees/reload")
	public String reloadEmployees(HttpSession session) {
		session.removeAttribute("temporaryEmployees");
		return "redirect:/employees";
	}

	@PostMapping("/employees/search")
	public String searchEmployees(@RequestParam String query, Model model, HttpSession session) {
		// Tìm kiếm trong danh sách chính
		List<NhanVien> searchResults = nhanVienService.searchEmployees(query);

		// Lấy danh sách tạm từ session
		@SuppressWarnings("unchecked")
		List<NhanVien> tempList = (List<NhanVien>) session.getAttribute("temporaryEmployees");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		// Kết hợp kết quả tìm kiếm với danh sách tạm
		List<NhanVien> allEmployees = new ArrayList<>(searchResults);
		allEmployees.addAll(tempList);

		// Tạo trang dữ liệu
		Page<NhanVien> employeePage = new PageImpl<>(allEmployees, PageRequest.of(0, Integer.MAX_VALUE), allEmployees.size());

		// Thêm vào model
		model.addAttribute("employees", employeePage);
		model.addAttribute("temporaryEmployees", tempList);
		model.addAttribute("canUndo", !nhanVienService.isUndoStackEmpty());

		if (allEmployees.isEmpty()) {
			model.addAttribute("message", "Không tìm thấy nhân viên nào.");
			model.addAttribute("messageType", "danger");
		}

		return "nhanvien/employee_list";
	}

	@PostMapping("/employees/undo")
	public String undoLastAction(RedirectAttributes redirectAttributes) {
		boolean success = nhanVienService.undoThaoTacCuoi();
		if (success) {
			redirectAttributes.addFlashAttribute("message", "Hoàn tác thành công");
			redirectAttributes.addFlashAttribute("messageType", "success");
		} else {
			redirectAttributes.addFlashAttribute("message", "Không có thao tác để hoàn tác");
			redirectAttributes.addFlashAttribute("messageType", "error");
		}
		return "redirect:/employees";
	}

	@PostMapping("/employees/clear-undo")
	public String clearUndoStackAndExit() {
		nhanVienService.clearUndoStack();
		return "redirect:/nhanvien/layout";
	}
}