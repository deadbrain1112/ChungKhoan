package chungkhoan.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;

import chungkhoan.entity.LenhDat;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.service.LenhDatService;
import jakarta.servlet.http.HttpSession;

@Controller
public class SaoKeController {
	
	@Autowired
	private LenhDatService lenhDatService;
	
	@PostMapping("/nhadautu/sao-ke-gdck")
	public String hienThiLenhDatTheoNDT(Model model, HttpSession session) {
	    NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");

	    if (nhaDauTu == null) {
	        return "nhanvien/login";
	    }

	    String maNDT = nhaDauTu.getMaNDT();
	    List<LenhDat> danhSach = lenhDatService.timTheoMaNhaDauTu(maNDT);

	    model.addAttribute("lenhDatList", danhSach);
	    model.addAttribute("nhaDauTu", nhaDauTu);
	    return "ndt/sao_ke_gdck";
	}
}
