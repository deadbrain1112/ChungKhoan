package chungkhoan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class StocksController {
	
	@GetMapping("/stocks")
    public String stocksList() {
    	return "stocks";
    }	
	
	// Hiển thị form thêm cổ phiếu 
	@GetMapping("/stocks/add")
	public String showAddStocks() {
		return "add_edit";
	}
	
	// Xử lý thêm cổ phiếu
	@PostMapping("/stocks/add")
	public String addStocks() {
		return "redirect:/stocks";
	}
	
	// Reload trang
	@GetMapping("/stocks/reload")
	public String reloadForm() {
		return "redirect:/stocks";
	}
	
	// Tìm kiếm cổ phiếu
	@PostMapping("/stocks/search")
	public String searching() {
		return "redirect:/stocks";
	}
	
	// Hiển thị form ghi cổ phiếu 
		@GetMapping("/stocks/edit/{maNDT}")
		public String showEditStocksForm() {
			return "add_edit";
		}
	
	// Ghi cổ phiếu
	@PostMapping("/stocks/edit/{maNDT}")
	public String editStocks() {
		return "redirect:/stocks";
	}
	
	// Xử lý xóa cổ phiếu
	@PostMapping("/stocks/delete/{maCP}")
	public String deleteStocks() {
		return "redirect:/stocks";
	}
	
	// Xử lý khôi phục cổ phiếu
	@GetMapping("/stocks/restore/{maCP}")
	public String restoreStocks() {
		return "redirect:/stocks";
	}
}
