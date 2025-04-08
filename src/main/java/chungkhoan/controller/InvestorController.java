package chungkhoan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.repository.NDTRepository;

@Controller
public class InvestorController {
	
	@Autowired
	private NDTRepository ndtRepository;
	
	@GetMapping("/investors")
    public String investorsList(Model model) {
		 model.addAttribute("investors", ndtRepository.findAll());
	     model.addAttribute("investor", new NhaDauTu()); // cho form Thêm
	     return "nhanvien/investor_list";
    }
	
	// Xử lý xóa nhà đầu tư ( xóa theo id trên đường dẫn )
	@PostMapping("/investors/delete/{maNDT}")
	public String deleteInvestor () {
		return "redirect:/investors";
	}
	
	// Xử lý thêm nhà đầu từ khi bấm Thêm. Bấm thêm --> tự đổi form action là /investors/add ( dòng 138 trong investor_list )
	@PostMapping("/investors/add")
	public String addInvestor() {
		return "redirect:/investors";
	}
	
	// Xử lý chỉnh sửa nhà đầu từ khi bấm Sửa. Bấm sửa --> tự đổi form action là /investors/edit + maNDT ( dòng 146 trong investor_list )
	@PostMapping("/investors/edit/{maNDT}")
	public String editInvestor() {
		return "redirect:/investors";
	}
	
}
