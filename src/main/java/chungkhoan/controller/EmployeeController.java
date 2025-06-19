package chungkhoan.controller;

import chungkhoan.dto.NhanVienTemp;
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

		List<NhanVien> fromDb = nhanVienService.getPaginated(0, Integer.MAX_VALUE).getContent();

		@SuppressWarnings("unchecked")
		List<NhanVienTemp> tempList = (List<NhanVienTemp>) session.getAttribute("temporaryEmployees");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		List<NhanVienTemp> finalTempList = tempList;
		List<NhanVien> filteredDb = fromDb.stream()
				.filter(nv -> finalTempList.stream().noneMatch(t -> t.getMaNV().equals(nv.getMaNV())))
				.toList();

		// Chuyển đổi danh sách tạm chưa bị xóa sang entity
		List<NhanVien> tempEntities = tempList.stream()
				.filter(t -> !t.isDaXoa())
				.map(NhanVienTemp::toNhanVienEntity)
				.toList();

		List<NhanVien> allEmployees = new ArrayList<>();
		allEmployees.addAll(filteredDb);
		allEmployees.addAll(tempEntities);

		// Tính toán phân trang
		int totalItems = allEmployees.size();
		int start = page * size;
		int end = Math.min(start + size, totalItems);

		if (start >= totalItems && totalItems > 0) {
			page = (totalItems - 1) / size;
			start = page * size;
			end = Math.min(start + size, totalItems);
		} else if (start >= totalItems) {
			start = 0;
			end = 0;
		}

		List<NhanVien> pageContent = (start < end) ? allEmployees.subList(start, end) : new ArrayList<>();

		Page<NhanVien> allEmployeesPage = new PageImpl<>(pageContent, PageRequest.of(page, size), totalItems);

		// Gửi dữ liệu ra view
		model.addAttribute("employees", allEmployeesPage);
		model.addAttribute("temporaryEmployees", tempList);
		model.addAttribute("canUndo", !nhanVienService.isUndoStackEmpty());

		if (allEmployeesPage.isEmpty()) {
			model.addAttribute("message", "Không có dữ liệu nhân viên.");
			model.addAttribute("messageType", "danger");
		}

		// Debug
		System.out.println("TempList size: " + tempList.size());
		System.out.println("AllEmployees size: " + allEmployees.size());
		System.out.println("Page content size: " + pageContent.size());

		return "nhanvien/employee_list";
	}

	@PostMapping("/employees/add-temp")
	public String addTempEmployee(@ModelAttribute NhanVienTemp employee,
								  @RequestParam(defaultValue = "0") int page,
								  @RequestParam(defaultValue = "5") int size,
								  HttpSession session,
								  RedirectAttributes redirectAttributes) {
		// Kiểm tra ngày sinh
		LocalDate ngaySinh = employee.getNgaySinh();
		if (ngaySinh == null || ngaySinh.isAfter(LocalDate.now()) || ngaySinh.getYear() < 1900) {
			redirectAttributes.addFlashAttribute("message", "Ngày sinh không hợp lệ!");
			redirectAttributes.addFlashAttribute("messageType", "danger");
			return "redirect:/employees?page=" + page + "&size=" + size;
		}

		@SuppressWarnings("unchecked")
		List<NhanVienTemp> tempList = (List<NhanVienTemp>) session.getAttribute("temporaryEmployees");
		if (tempList == null) tempList = new ArrayList<>();

		// Kiểm tra trùng mã nhân viên
		boolean maNVTrung = nhanVienService.existsById(employee.getMaNV()) ||
				tempList.stream()
						.filter(temp -> !temp.isDaXoa())
						.anyMatch(temp -> temp.getMaNV().equals(employee.getMaNV()));
		if (maNVTrung) {
			redirectAttributes.addFlashAttribute("message", "Mã nhân viên " + employee.getMaNV() + " đã tồn tại!");
			redirectAttributes.addFlashAttribute("messageType", "error");
			return "redirect:/employees?page=" + page + "&size=" + size;
		}

		// Kiểm tra trùng CMND
		boolean cmndTrung = nhanVienService.existsByCmnd(employee.getCmnd()) ||
				tempList.stream()
						.filter(temp -> !temp.isDaXoa())
						.anyMatch(temp -> temp.getCmnd().equals(employee.getCmnd()));
		if (cmndTrung) {
			redirectAttributes.addFlashAttribute("message", "CMND " + employee.getCmnd() + " đã tồn tại!");
			redirectAttributes.addFlashAttribute("messageType", "error");
			return "redirect:/employees?page=" + page + "&size=" + size;
		}

		// Thêm vào danh sách tạm
		tempList.add(employee);
		session.setAttribute("temporaryEmployees", tempList);
		redirectAttributes.addFlashAttribute("message", "Đã thêm tạm nhân viên " + employee.getMaNV());
		redirectAttributes.addFlashAttribute("messageType", "success");

		return "redirect:/employees?page=" + page + "&size=" + size;
	}

	@PostMapping("/employees/edit-temp")
	public String editTempEmployee(@ModelAttribute NhanVienTemp employee,
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
		List<NhanVienTemp> tempList = (List<NhanVienTemp>) session.getAttribute("temporaryEmployees");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		Optional<NhanVien> existing = nhanVienService.findById(employee.getMaNV());
		boolean cmndChanged = existing.isPresent() && !existing.get().getCmnd().equals(employee.getCmnd());

		if (cmndChanged || tempList.stream()
				.filter(emp -> !emp.getMaNV().equals(employee.getMaNV()))
				.anyMatch(emp -> emp.getCmnd().equals(employee.getCmnd()))) {
			redirectAttributes.addFlashAttribute("message", "CMND " + employee.getCmnd() + " đã tồn tại!");
			redirectAttributes.addFlashAttribute("messageType", "error");
			return "redirect:/employees";
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
		List<NhanVienTemp> tempList = (List<NhanVienTemp>) session.getAttribute("temporaryEmployees");

		if (tempList != null && !tempList.isEmpty()) {
			try {
				for (NhanVienTemp temp : tempList) {
					if (temp.isDaXoa()) {
						// Xóa nhân viên nếu được đánh dấu xóa
						nhanVienService.xoaNhanVien(temp.getMaNV());
					} else {
						Optional<NhanVien> existing = nhanVienService.findById(temp.getMaNV());
						if (existing.isPresent()) {
							// Cập nhật nếu đã tồn tại
							nhanVienService.capNhatNhanVien(temp.getMaNV(), temp.toNhanVienEntity());
						} else {
							// Thêm mới nếu chưa có
							nhanVienService.themNhanVienBangSP(temp.toNhanVienEntity());
						}
					}
				}
				session.removeAttribute("temporaryEmployees");
				redirectAttributes.addFlashAttribute("message", "Đã ghi vào cơ sở dữ liệu!");
				redirectAttributes.addFlashAttribute("messageType", "success");
			} catch (Exception e) {
				redirectAttributes.addFlashAttribute("message", "Lỗi khi lưu nhân viên: " + e.getMessage());
				redirectAttributes.addFlashAttribute("messageType", "error");
			}
		} else {
			redirectAttributes.addFlashAttribute("message", "Không có nhân viên tạm để ghi.");
			redirectAttributes.addFlashAttribute("messageType", "error");
		}
		return "redirect:/employees";
	}

	@PostMapping("/employees/delete")
	public String markEmployeeDeleted(@RequestParam String maNV, HttpSession session, RedirectAttributes redirectAttributes) {
		@SuppressWarnings("unchecked")
		List<NhanVienTemp> tempList = (List<NhanVienTemp>) session.getAttribute("temporaryEmployees");
		if (tempList == null) tempList = new ArrayList<>();

		Optional<NhanVienTemp> temp = tempList.stream().filter(t -> t.getMaNV().equals(maNV)).findFirst();
		if (temp.isPresent()) {
			temp.get().setDaXoa(true);
		} else {
			Optional<NhanVien> nv = nhanVienService.findById(maNV);
			List<NhanVienTemp> finalTempList = tempList;
			nv.ifPresent(n -> {
				NhanVienTemp t = new NhanVienTemp(n);
				t.setDaXoa(true);
				finalTempList.add(t);
			});
		}

		session.setAttribute("temporaryEmployees", tempList);
		redirectAttributes.addFlashAttribute("message", "Đã đánh dấu xóa nhân viên " + maNV);
		redirectAttributes.addFlashAttribute("messageType", "info");

		return "redirect:/employees";
	}

	@PostMapping("/employees/remove-temp")
	public String removeTempEmployee(@RequestParam String maNV, HttpSession session, RedirectAttributes redirectAttributes) {
		@SuppressWarnings("unchecked")
		List<NhanVienTemp> tempList = (List<NhanVienTemp>) session.getAttribute("temporaryEmployees");
		if (tempList != null) {
			boolean found = false;
			for (NhanVienTemp emp : tempList) {
				if (emp.getMaNV().equals(maNV)) {
					emp.setDaXoa(true);
					found = true;
					break;
				}
			}
			session.setAttribute("temporaryEmployees", tempList);
			if (found) {
				redirectAttributes.addFlashAttribute("message", "Đã đánh dấu xóa nhân viên tạm có mã " + maNV);
				redirectAttributes.addFlashAttribute("messageType", "success");
			} else {
				redirectAttributes.addFlashAttribute("message", "Không tìm thấy nhân viên tạm có mã " + maNV);
				redirectAttributes.addFlashAttribute("messageType", "warning");
			}
		} else {
			redirectAttributes.addFlashAttribute("message", "Không có danh sách nhân viên tạm.");
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
		List<NhanVienTemp> tempList = (List<NhanVienTemp>) session.getAttribute("temporaryEmployees");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		// Thêm các nhân viên tạm không bị đánh dấu xóa và có chứa từ khóa
		for (NhanVienTemp temp : tempList) {
			if (!temp.isDaXoa()) {
				boolean match = temp.getMaNV().toLowerCase().contains(query.toLowerCase())
						|| temp.getHoTen().toLowerCase().contains(query.toLowerCase())
						|| temp.getCmnd().toLowerCase().contains(query.toLowerCase());
				if (match) {
					searchResults.add(temp.toNhanVienEntity());
				}
			}
		}

		// Gói lại kết quả tìm kiếm vào Page
		Page<NhanVien> employeePage = new PageImpl<>(searchResults, PageRequest.of(0, Integer.MAX_VALUE), searchResults.size());

		model.addAttribute("employees", employeePage);
		model.addAttribute("temporaryEmployees", tempList);
		model.addAttribute("canUndo", !nhanVienService.isUndoStackEmpty());

		if (searchResults.isEmpty()) {
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