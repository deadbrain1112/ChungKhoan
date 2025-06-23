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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

		// Lọc những nhân viên từ DB không trùng mã với danh sách tạm
		List<NhanVienTemp> finalTempList = tempList;
		List<NhanVien> filteredDb = fromDb.stream()
				.filter(nv -> finalTempList.stream().noneMatch(t -> t.getMaNV().equals(nv.getMaNV())))
				.toList();

		// Chuyển tất cả nhân viên từ DB sang Temp
		List<NhanVienTemp> allEmployees = new ArrayList<>();

		for (NhanVien nv : filteredDb) {
			allEmployees.add(new NhanVienTemp(nv));
		}

		allEmployees.addAll(tempList);

		// Phân trang
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

		List<NhanVienTemp> pageContent = (start < end) ? allEmployees.subList(start, end) : new ArrayList<>();

		Page<NhanVienTemp> allEmployeesPage = new PageImpl<>(pageContent, PageRequest.of(page, size), totalItems);

		// Gửi dữ liệu ra view
		model.addAttribute("employees", allEmployeesPage);
		model.addAttribute("temporaryEmployees", tempList);
		model.addAttribute("canUndo", !nhanVienService.isUndoStackEmpty());

		if (allEmployeesPage.isEmpty()) {
			model.addAttribute("message", "Không có dữ liệu nhân viên.");
			model.addAttribute("messageType", "danger");
		}

		List<String> temporaryMaNVList = tempList.stream()
				.filter(t -> !t.isDaXoa())
				.map(NhanVienTemp::getMaNV)
				.toList();
		model.addAttribute("temporaryMaNVList", temporaryMaNVList);

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
	public String removeTempEmployee(@RequestParam String maNV,
									 HttpSession session,
									 RedirectAttributes redirectAttributes) {
		@SuppressWarnings("unchecked")
		List<NhanVienTemp> tempList = (List<NhanVienTemp>) session.getAttribute("temporaryEmployees");

		if (tempList != null) {
			// Loại bỏ nhân viên có mã maNV ra khỏi danh sách
			boolean removed = tempList.removeIf(emp -> emp.getMaNV().equals(maNV));
			session.setAttribute("temporaryEmployees", tempList);

			if (removed) {
				redirectAttributes.addFlashAttribute("message", "Đã xóa khỏi danh sách tạm nhân viên có mã " + maNV);
				redirectAttributes.addFlashAttribute("messageType", "success");
			} else {
				redirectAttributes.addFlashAttribute("message", "Không tìm thấy nhân viên tạm có mã " + maNV);
				redirectAttributes.addFlashAttribute("messageType", "warning");
			}
		} else {
			redirectAttributes.addFlashAttribute("message", "Danh sách tạm không tồn tại.");
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
		List<NhanVien> dbResults = nhanVienService.searchEmployees(query);

		@SuppressWarnings("unchecked")
		List<NhanVienTemp> tempList = (List<NhanVienTemp>) session.getAttribute("temporaryEmployees");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		// Chuyển dbResults thành NhanVienTemp
		List<NhanVienTemp> searchResults = dbResults.stream()
				.map(NhanVienTemp::new)
				.collect(Collectors.toList());

		// Thêm các nhân viên tạm không bị xóa và khớp từ khóa
		for (NhanVienTemp temp : tempList) {
			if (!temp.isDaXoa()) {
				boolean match = temp.getMaNV().toLowerCase().contains(query.toLowerCase())
						|| temp.getHoTen().toLowerCase().contains(query.toLowerCase())
						|| temp.getCmnd().toLowerCase().contains(query.toLowerCase());
				if (match) {
					searchResults.add(temp);
				}
			}
		}

		// Gói kết quả vào Page<NhanVienTemp>
		Page<NhanVienTemp> employeePage = new PageImpl<>(searchResults, PageRequest.of(0, Integer.MAX_VALUE), searchResults.size());

		// Thêm temporaryMaNVList
		List<String> temporaryMaNVList = tempList.stream()
				.filter(t -> !t.isDaXoa())
				.map(NhanVienTemp::getMaNV)
				.toList();

		model.addAttribute("employees", employeePage);
		model.addAttribute("temporaryEmployees", tempList);
		model.addAttribute("temporaryMaNVList", temporaryMaNVList);
		model.addAttribute("canUndo", !nhanVienService.isUndoStackEmpty());

		if (searchResults.isEmpty()) {
			model.addAttribute("message", "Không tìm thấy nhân viên nào.");
			model.addAttribute("messageType", "danger");
		}

		// Logging để debug
		System.out.println("searchEmployees - searchResults size: " + searchResults.size());
		System.out.println("searchEmployees - temporaryMaNVList: " + temporaryMaNVList);
		System.out.println("searchEmployees - temporaryEmployees size: " + tempList.size());
		System.out.println("searchEmployees - employees class: " + employeePage.getClass().getName());
		System.out.println("searchEmployees - employees.content size: " + employeePage.getContent().size());

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

	@PostMapping("/employees/undo-delete")
	public String undoDelete(@RequestParam("maNV") String maNV, HttpSession session,
							 @RequestParam(defaultValue = "0") int page,
							 @RequestParam(defaultValue = "5") int size,
							 RedirectAttributes redirectAttributes) {

		@SuppressWarnings("unchecked")
		List<NhanVienTemp> tempList = (List<NhanVienTemp>) session.getAttribute("temporaryEmployees");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		Iterator<NhanVienTemp> iterator = tempList.iterator();
		while (iterator.hasNext()) {
			NhanVienTemp temp = iterator.next();
			if (temp.getMaNV().equals(maNV) && temp.isDaXoa()) {
				iterator.remove();
				redirectAttributes.addFlashAttribute("message", "Đã hoàn tác xóa cho nhân viên " + maNV);
				redirectAttributes.addFlashAttribute("messageType", "success");
				break;
			}
		}

		session.setAttribute("temporaryEmployees", tempList);
		return "redirect:/employees?page=" + page + "&size=" + size;
	}

	@PostMapping("/employees/clear-undo")
	public String clearUndoStackAndExit() {
		nhanVienService.clearUndoStack();
		return "redirect:/nhanvien/layout";
	}
}