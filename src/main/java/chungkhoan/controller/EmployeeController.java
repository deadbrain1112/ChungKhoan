package chungkhoan.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.NhanVien;
import chungkhoan.repository.NhanVienRepository;

@Controller
public class EmployeeController {
	
	@Autowired NhanVienRepository nhanVienRepository;
	
	@GetMapping("/employees")
	public String employeeList(Model model) {
		List<NhanVien> list = nhanVienRepository.findAll();
		model.addAttribute("employees", list);
		model.addAttribute("employee", new NhanVien()); // cho form thêm
		
		if (list.isEmpty()) {
			model.addAttribute("noDataMessage", "Không có dữ liệu nhân viên.");
		}
		
		return "nhanvien/employee_list";
	}
}
