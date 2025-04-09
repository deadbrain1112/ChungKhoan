package chungkhoan.controller;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.repository.NDTRepository;
import chungkhoan.service.NDTService;

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

	// Hiển thị danh sách nhà đầu tư và form thêm mới
	@GetMapping("/investors")
	public String investorsList(Model model) {
		model.addAttribute("investors", ndtRepository.findAll());
		model.addAttribute("investor", new NhaDauTu()); // cho form thêm
		model.addAttribute("disablePassword", false);   // mặc định là false
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
		// Gán mã NDT vào để tránh bị null
		updatedInvestor.setMaNDT(maNDT);
		ndtRepository.save(updatedInvestor);
		return "redirect:/investors";
	}
}
