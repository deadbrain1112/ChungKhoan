package chungkhoan.controller;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.repository.NDTRepository;
import chungkhoan.service.NDTService;
import chungkhoan.service.TaiKhoanNganHangService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class InvestorController {

	@Autowired
	private NDTRepository ndtRepository;

	@Autowired
	private NDTService ndtService;
	
	@Autowired
	private TaiKhoanNganHangService taiKhoanNganHangService;

	// Hiển thị danh sách nhà đầu tư và form thêm mới
	@GetMapping("/investors")
	public String investorsList(@RequestParam(value = "edit", required = false) String maNDT, Model model) {
		List<NhaDauTu> list = ndtRepository.findAll();
		list.forEach(ndt -> ndt.setMkGiaoDich(null)); // cho mkgd bằng null trên view
		model.addAttribute("investors", list);
		model.addAttribute("investor", new NhaDauTu()); // cho form thêm
		
		if (list.isEmpty()) {
			model.addAttribute("noDataMessage", "Không có dữ liệu nhà đầu tư.");
		}
		
		// Nếu có ?edit=maNDT thì xử lý form sửa
		if (maNDT != null) {
			NhaDauTu editInvestor = ndtRepository.findById(maNDT).orElse(null);
			model.addAttribute("editInvestor", editInvestor);

			TaiKhoanNganHang taiKhoan = taiKhoanNganHangService.getTaiKhoanByNDT(editInvestor);
			if (taiKhoan == null) {
	            model.addAttribute("nullTKNH", "Không tìm thấy thông tin tài khoản!");
	        }
			
			model.addAttribute("taiKhoan", taiKhoan);
		}
		
		return "nhanvien/investor_list";
	}

	// Xử lý thêm nhà đầu tư (gọi stored procedure)
	@PostMapping("/investors/add")
	public String addInvestor(@ModelAttribute("investor") NhaDauTu investor) {
		ndtService.themNhaDauTuBangSP(investor);
		return "redirect:/investors";
	}

	// Xử lý xóa nhà đầu tư
	@PostMapping("/investors/delete/{maNDT}")
	public String deleteInvestor(@PathVariable("maNDT") String maNDT) {
		ndtRepository.deleteById(maNDT);
		return "redirect:/investors";
	}

	// Xử lý sửa nhà đầu tư
	@PostMapping("/investors/edit/{maNDT}")
	public String editInvestor(@PathVariable("maNDT") String maNDT,
							   @ModelAttribute("investor") NhaDauTu updatedInvestor) {
		// Lấy đối tượng gốc từ DB để giữ lại giá trị mkgd
		NhaDauTu existingInvestor = ndtRepository.findById(maNDT).orElse(null);
		
		if (existingInvestor != null) {
			// Gán lại các giá trị không có trên form để tránh bị null
			updatedInvestor.setMkGiaoDich(existingInvestor.getMkGiaoDich());

			// Gán lại mã nhà đầu tư
			updatedInvestor.setMaNDT(maNDT);

			ndtRepository.save(updatedInvestor);
		}
		
		return "redirect:/investors";
	}
}
