package chungkhoan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class InvestorController {
	
	@GetMapping("/investors")
    public String investorsList() {
    	return "investor_list";
    }
	
	// Xử lý xóa nhà đầu tư ( xóa theo id trên đường dẫn )
	@PostMapping("/investors/delete/{maNDT}")
	public String deleteInvestor () {
		return "redirect:/investos_list";
	}
	
	// Xử lý thêm nhà đầu từ khi bấm Thêm. Bấm thêm --> tự đổi form action là /investors/add ( dòng 138 trong investor_list )
	@PostMapping("/investors/add")
	public String addInvestor() {
		return "redirect:/investor_list";
	}
	
	// Xử lý chỉnh sửa nhà đầu từ khi bấm Sửa. Bấm sửa --> tự đổi form action là /investors/edit + maNDT ( dòng 146 trong investor_list )
	@PostMapping("/investors/edit/{maNDT}")
	public String editInvestor() {
		return "redirect:/investor_list";
	}
	
}
