package chungkhoan.controller;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.repository.NDTRepository;
import chungkhoan.repository.TaiKhoanNganHangRepository;
import chungkhoan.service.NDTService;
import chungkhoan.service.TaiKhoanNganHangService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

	@Autowired
	private TaiKhoanNganHangRepository tkNganHangRepo;

	// Hiển thị danh sách nhà đầu tư và form thêm mới
	@GetMapping("/investors")
	public String investorsList(@RequestParam(value = "edit", required = false) String maNDT, Model model) {
	    List<NhaDauTu> list = ndtRepository.findAll();
	    Map<String, List<TaiKhoanNganHang>> bankAccountMap = new HashMap<>();

	    // Xóa mật khẩu giao dịch trước khi hiển thị
	    list.forEach(ndt -> ndt.setMkGiaoDich(null));
	    model.addAttribute("investors", list);
	    model.addAttribute("investor", new NhaDauTu()); // Dùng cho form thêm

	    // Lấy danh sách tài khoản ngân hàng cho từng nhà đầu tư
	    for (NhaDauTu ndt : list) {
	        List<TaiKhoanNganHang> accounts = tkNganHangRepo.findByNhaDauTu(ndt);
	        bankAccountMap.put(ndt.getMaNDT(), accounts);
	    }
	    model.addAttribute("bankAccountMap", bankAccountMap); // Đưa vào model để render data-* trong view

	    // Nếu không có dữ liệu nhà đầu tư
	    if (list.isEmpty()) {
	        model.addAttribute("noDataMessage", "Không có dữ liệu nhà đầu tư.");
	    }

	    // Nếu có yêu cầu sửa
	    if (maNDT != null) {
	        NhaDauTu editInvestor = ndtRepository.findById(maNDT).orElse(null);
	        model.addAttribute("editInvestor", editInvestor);

	        if (editInvestor != null) {
	            List<TaiKhoanNganHang> taiKhoans = tkNganHangRepo.findByNhaDauTu(editInvestor);
	            model.addAttribute("editTaiKhoans", taiKhoans);
	        } else {
	            model.addAttribute("nullTKNH", "Không tìm thấy nhà đầu tư hoặc tài khoản!");
	        }
	    }

	    return "nhanvien/investor_list";
	}

	@PostMapping("/investors/undo")
	public ResponseEntity<String> undoLastAction() {
		boolean success = ndtService.undoThaoTacCuoi();
		return success ? ResponseEntity.ok("Undo thành công") : ResponseEntity.badRequest().body("Không có thao tác để hoàn tác");
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
		NhaDauTu existingInvestor = ndtRepository.findById(maNDT).orElse(null);

		if (existingInvestor != null) {
			updatedInvestor.setMkGiaoDich(existingInvestor.getMkGiaoDich());

			// Gán lại mã nhà đầu tư
			updatedInvestor.setMaNDT(maNDT);

			ndtRepository.save(updatedInvestor);
		}

		return "redirect:/investors";
	}
}
