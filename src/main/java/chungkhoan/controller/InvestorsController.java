package chungkhoan.controller;

import chungkhoan.dto.NhaDauTuTemp;
import chungkhoan.entity.NganHang;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.repository.LenhDatRepository;
import chungkhoan.repository.NDTRepository;
import chungkhoan.service.NDTService;
import chungkhoan.service.NganHangService;
import chungkhoan.service.TaiKhoanNganHangService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class InvestorsController {

	@Autowired
	private NDTService ndtService;
	
	@Autowired
	private TaiKhoanNganHangService taiKhoanNganHangService;
	
	@Autowired
	private NganHangService nganHangService;
	
	@Autowired 
	private NDTRepository ndtRepository;
	
	@Autowired
	private LenhDatRepository lenhDatRepository;

	@GetMapping("/investors")
	public String InvestorList(@RequestParam(defaultValue = "0") int page,
							   @RequestParam(defaultValue = "5") int size,
							   Model model,
							   HttpSession session) {
		
		if (session.getAttribute("nhanVien") == null) {
	        return "redirect:/login";
	    }

		List<NhaDauTu> fromDb = ndtService.getPaginated(0, Integer.MAX_VALUE).getContent();

		@SuppressWarnings("unchecked")
		List<NhaDauTuTemp> tempList = (List<NhaDauTuTemp>) session.getAttribute("temporaryInvestors");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		List<NhaDauTuTemp> finalTempList = tempList;
		List<NhaDauTu> filteredDb = fromDb.stream()
				.filter(nv -> finalTempList.stream().noneMatch(t -> t.getMaNDT().equals(nv.getMaNDT())))
				.toList();

		List<NhaDauTuTemp> allinvestors = new ArrayList<>();
		for (NhaDauTu ndt : filteredDb) {
			allinvestors.add(new NhaDauTuTemp(ndt));
		}
		allinvestors.addAll(tempList);

		int totalItems = allinvestors.size();
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

		List<NhaDauTuTemp> pageContent = (start < end) ? allinvestors.subList(start, end) : new ArrayList<>();
		Page<NhaDauTuTemp> allinvestorsPage = new PageImpl<>(pageContent, PageRequest.of(page, size), totalItems);

		model.addAttribute("investors", allinvestorsPage);
		model.addAttribute("temporaryInvestors", tempList);
		model.addAttribute("canUndo", !ndtService.isUndoStackEmpty());

		if (allinvestorsPage.isEmpty()) {
			model.addAttribute("message", "Không có dữ liệu nhà đầu tư.");
			model.addAttribute("messageType", "danger");
		}

		List<String> temporaryMaNDTList = tempList.stream()
				.filter(t -> !t.isDaXoa())
				.map(NhaDauTuTemp::getMaNDT)
				.toList();
		model.addAttribute("temporaryMaNDTList", temporaryMaNDTList);

		// Logging để debug
		System.out.println("InvestorList - TempList size: " + tempList.size());
		System.out.println("InvestorList - Allinvestors size: " + allinvestors.size());
		System.out.println("InvestorList - Page content size: " + pageContent.size());
		System.out.println("InvestorList - temporaryMaNDTList: " + temporaryMaNDTList);
		System.out.println("InvestorList - investors class: " + allinvestorsPage.getClass().getName());
		System.out.println("InvestorList - investors.content size: " + allinvestorsPage.getContent().size());
		
		model.addAttribute("dsNganHang", nganHangService.findAll());

		return "nhanvien/investor_list";
	}

	@PostMapping("/investors/add-temp")
	public String addTempInvestor(@ModelAttribute NhaDauTuTemp investor,
								  @RequestParam(defaultValue = "0") int page,
								  @RequestParam(defaultValue = "5") int size,
								  HttpSession session,
								  RedirectAttributes redirectAttributes) {
		LocalDate ngaySinh = investor.getNgaySinh();
		if (ngaySinh == null || ngaySinh.isAfter(LocalDate.now()) || ngaySinh.getYear() < 1900) {
			redirectAttributes.addFlashAttribute("message", "Ngày sinh không hợp lệ!");
			redirectAttributes.addFlashAttribute("messageType", "danger");
			return "redirect:/investors?page=" + page + "&size=" + size;
		}

		@SuppressWarnings("unchecked")
		List<NhaDauTuTemp> tempList = (List<NhaDauTuTemp>) session.getAttribute("temporaryInvestors");
		if (tempList == null) tempList = new ArrayList<>();

		boolean maNDTTrung = ndtService.existsById(investor.getMaNDT()) ||
				tempList.stream()
						.filter(temp -> !temp.isDaXoa())
						.anyMatch(temp -> temp.getMaNDT().equals(investor.getMaNDT()));
		if (maNDTTrung) {
			redirectAttributes.addFlashAttribute("message", "Mã nhà đầu tư " + investor.getMaNDT() + " đã tồn tại!");
			redirectAttributes.addFlashAttribute("messageType", "error");
			return "redirect:/investors?page=" + page + "&size=" + size;
		}

		boolean cmndTrung = ndtService.existsByCmnd(investor.getCmnd()) ||
				tempList.stream()
						.filter(temp -> !temp.isDaXoa())
						.anyMatch(temp -> temp.getCmnd().equals(investor.getCmnd()));
		if (cmndTrung) {
			redirectAttributes.addFlashAttribute("message", "CMND " + investor.getCmnd() + " đã tồn tại!");
			redirectAttributes.addFlashAttribute("messageType", "error");
			return "redirect:/investors?page=" + page + "&size=" + size;
		}

		tempList.add(investor);
		session.setAttribute("temporaryInvestors", tempList);
		redirectAttributes.addFlashAttribute("message", "Đã thêm tạm nhà đầu tư " + investor.getMaNDT());
		redirectAttributes.addFlashAttribute("messageType", "success");

		return "redirect:/investors?page=" + page + "&size=" + size;
	}

	@PostMapping("/investors/edit-temp")
	public String editTempInvestor(@ModelAttribute NhaDauTuTemp investor,
								   HttpSession session,
								   RedirectAttributes redirectAttributes) {
		LocalDate ngaySinh = investor.getNgaySinh();
		LocalDate currentDate = LocalDate.now();
		if (ngaySinh == null || ngaySinh.isAfter(currentDate) || ngaySinh.getYear() < 1900) {
			redirectAttributes.addFlashAttribute("message", "Ngày sinh không hợp lệ!");
			redirectAttributes.addFlashAttribute("messageType", "danger");
			return "redirect:/investors";
		}

		@SuppressWarnings("unchecked")
		List<NhaDauTuTemp> tempList = (List<NhaDauTuTemp>) session.getAttribute("temporaryInvestors");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		Optional<NhaDauTu> existing = ndtService.findById(investor.getMaNDT());
		boolean cmndChanged = existing.isPresent() && !existing.get().getCmnd().equals(investor.getCmnd());

		if (cmndChanged || tempList.stream()
				.filter(emp -> !emp.getMaNDT().equals(investor.getMaNDT()))
				.anyMatch(emp -> emp.getCmnd().equals(investor.getCmnd()))) {
			redirectAttributes.addFlashAttribute("message", "CMND " + investor.getCmnd() + " đã tồn tại!");
			redirectAttributes.addFlashAttribute("messageType", "error");
			return "redirect:/investors";
		}

		boolean replaced = false;
		for (int i = 0; i < tempList.size(); i++) {
			if (tempList.get(i).getMaNDT().equals(investor.getMaNDT())) {
				tempList.set(i, investor);
				replaced = true;
				break;
			}
		}
		if (!replaced) {
			tempList.add(investor);
		}

		session.setAttribute("temporaryInvestors", tempList);
		redirectAttributes.addFlashAttribute("message", "Đã cập nhật tạm nhà đầu tư " + investor.getMaNDT());
		redirectAttributes.addFlashAttribute("messageType", "success");

		return "redirect:/investors";
	}

	@PostMapping("/investors/save-all")
	public String saveAllInvestors(HttpSession session, RedirectAttributes redirectAttributes) {
		@SuppressWarnings("unchecked")
		List<NhaDauTuTemp> tempList = (List<NhaDauTuTemp>) session.getAttribute("temporaryInvestors");

		if (tempList != null && !tempList.isEmpty()) {
			try {
				for (NhaDauTuTemp temp : tempList) {
					if (temp.isDaXoa()) {
						ndtService.xoaNhaDauTu(temp.getMaNDT());
					} else {
						Optional<NhaDauTu> existing = ndtService.findById(temp.getMaNDT());
						if (existing.isPresent()) {
							ndtService.capNhatNhaDauTu(temp.getMaNDT(), temp.toNhaDauTuEntity());
						} else {
							ndtService.themNhaDauTuBangSP(temp.toNhaDauTuEntity());
						}
					}
				}
				session.removeAttribute("temporaryInvestors");
				redirectAttributes.addFlashAttribute("message", "Đã ghi vào cơ sở dữ liệu!");
				redirectAttributes.addFlashAttribute("messageType", "success");
			} catch (Exception e) {
				redirectAttributes.addFlashAttribute("message", "Lỗi khi lưu nhà đầu tư: " + e.getMessage());
				redirectAttributes.addFlashAttribute("messageType", "error");
			}
		} else {
			redirectAttributes.addFlashAttribute("message", "Không có nhà đầu tư tạm để ghi.");
			redirectAttributes.addFlashAttribute("messageType", "error");
		}
		return "redirect:/investors";
	}

	@PostMapping("/investors/delete")
	public String markInvestorDeleted(@RequestParam String maNDT, HttpSession session, RedirectAttributes redirectAttributes) {
		
		if (lenhDatRepository.existsByNhaDauTu(maNDT)) {
	        redirectAttributes.addFlashAttribute("message", "Không thể xóa nhà đầu tư '" + maNDT + "' vì đã thực hiện giao dịch.");
	        redirectAttributes.addFlashAttribute("messageType", "error");
	        return "redirect:/investors";
	    }
		
		@SuppressWarnings("unchecked")
		List<NhaDauTuTemp> tempList = (List<NhaDauTuTemp>) session.getAttribute("temporaryInvestors");
		if (tempList == null) tempList = new ArrayList<>();

		Optional<NhaDauTuTemp> temp = tempList.stream().filter(t -> t.getMaNDT().equals(maNDT)).findFirst();
		if (temp.isPresent()) {
			temp.get().setDaXoa(true);
		} else {
			Optional<NhaDauTu> nv = ndtService.findById(maNDT);
			List<NhaDauTuTemp> finalTempList = tempList;
			nv.ifPresent(n -> {
				NhaDauTuTemp t = new NhaDauTuTemp(n);
				t.setDaXoa(true);
				finalTempList.add(t);
			});
		}

		session.setAttribute("temporaryInvestors", tempList);
		redirectAttributes.addFlashAttribute("message", "Đã đánh dấu xóa nhà đầu tư " + maNDT);
		redirectAttributes.addFlashAttribute("messageType", "info");

		return "redirect:/investors";
	}

	@PostMapping("/investors/remove-temp")
	public String removeTempInvestor(@RequestParam String maNDT,
									 HttpSession session,
									 RedirectAttributes redirectAttributes) {
		@SuppressWarnings("unchecked")
		List<NhaDauTuTemp> tempList = (List<NhaDauTuTemp>) session.getAttribute("temporaryInvestors");

		if (tempList != null) {
			boolean removed = tempList.removeIf(emp -> emp.getMaNDT().equals(maNDT));
			session.setAttribute("temporaryInvestors", tempList);

			if (removed) {
				redirectAttributes.addFlashAttribute("message", "Đã xóa khỏi danh sách tạm nhà đầu tư có mã " + maNDT);
				redirectAttributes.addFlashAttribute("messageType", "success");
			} else {
				redirectAttributes.addFlashAttribute("message", "Không tìm thấy nhà đầu tư tạm có mã " + maNDT);
				redirectAttributes.addFlashAttribute("messageType", "warning");
			}
		} else {
			redirectAttributes.addFlashAttribute("message", "Danh sách tạm không tồn tại.");
			redirectAttributes.addFlashAttribute("messageType", "error");
		}

		return "redirect:/investors";
	}

	@GetMapping("/investors/reload")
	public String reloadInvestors(HttpSession session) {
		session.removeAttribute("temporaryInvestors");
		return "redirect:/investors";
	}

	@PostMapping("/investors/search")
	public String searchInvestors(@RequestParam String query, Model model, HttpSession session) {
		List<NhaDauTu> dbResults = ndtService.searchInvestors(query);

		@SuppressWarnings("unchecked")
		List<NhaDauTuTemp> tempList = (List<NhaDauTuTemp>) session.getAttribute("temporaryInvestors");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		// Chuyển dbResults thành NhaDauTuTemp
		List<NhaDauTuTemp> searchResults = dbResults.stream()
				.map(NhaDauTuTemp::new)
				.collect(Collectors.toList());

		// Thêm các nhà đầu tư tạm không bị xóa và khớp từ khóa
		for (NhaDauTuTemp temp : tempList) {
			if (!temp.isDaXoa()) {
				boolean match = temp.getMaNDT().toLowerCase().contains(query.toLowerCase())
						|| temp.getHoTen().toLowerCase().contains(query.toLowerCase())
						|| temp.getCmnd().toLowerCase().contains(query.toLowerCase());
				if (match) {
					searchResults.add(temp);
				}
			}
		}

		// Gói kết quả vào Page<NhaDauTuTemp>
		Page<NhaDauTuTemp> investorPage = new PageImpl<>(searchResults, PageRequest.of(0, Integer.MAX_VALUE), searchResults.size());

		// Thêm temporaryMaNDTList
		List<String> temporaryMaNDTList = tempList.stream()
				.filter(t -> !t.isDaXoa())
				.map(NhaDauTuTemp::getMaNDT)
				.toList();

		model.addAttribute("investors", investorPage);
		model.addAttribute("temporaryInvestors", tempList);
		model.addAttribute("temporaryMaNDTList", temporaryMaNDTList);
		model.addAttribute("canUndo", !ndtService.isUndoStackEmpty());

		if (searchResults.isEmpty()) {
			model.addAttribute("message", "Không tìm thấy nhà đầu tư nào.");
			model.addAttribute("messageType", "danger");
		}

		// Logging để debug
		System.out.println("searchInvestors - searchResults size: " + searchResults.size());
		System.out.println("searchInvestors - temporaryMaNDTList: " + temporaryMaNDTList);
		System.out.println("searchInvestors - temporaryInvestors size: " + tempList.size());
		System.out.println("searchInvestors - investors class: " + investorPage.getClass().getName());
		System.out.println("searchInvestors - investors.content size: " + investorPage.getContent().size());

		return "nhanvien/investor_list";
	}

	@PostMapping("/investors/undo")
	public String undoLastAction(RedirectAttributes redirectAttributes) {
		boolean success = ndtService.undoThaoTacCuoi();
		if (success) {
			redirectAttributes.addFlashAttribute("message", "Hoàn tác thành công");
			redirectAttributes.addFlashAttribute("messageType", "success");
		} else {
			redirectAttributes.addFlashAttribute("message", "Không có thao tác để hoàn tác");
			redirectAttributes.addFlashAttribute("messageType", "error");
		}
		return "redirect:/investors";
	}

	@PostMapping("/investors/undo-delete")
	public String undoDelete(@RequestParam("maNDT") String maNDT, HttpSession session,
							 @RequestParam(defaultValue = "0") int page,
							 @RequestParam(defaultValue = "5") int size,
							 RedirectAttributes redirectAttributes) {

		@SuppressWarnings("unchecked")
		List<NhaDauTuTemp> tempList = (List<NhaDauTuTemp>) session.getAttribute("temporaryInvestors");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		Iterator<NhaDauTuTemp> iterator = tempList.iterator();
		while (iterator.hasNext()) {
			NhaDauTuTemp temp = iterator.next();
			if (temp.getMaNDT().equals(maNDT) && temp.isDaXoa()) {
				iterator.remove();
				redirectAttributes.addFlashAttribute("message", "Đã hoàn tác xóa cho nhà đầu tư " + maNDT);
				redirectAttributes.addFlashAttribute("messageType", "success");
				break;
			}
		}

		session.setAttribute("temporaryInvestors", tempList);
		return "redirect:/investors?page=" + page + "&size=" + size;
	}

	@PostMapping("/investors/clear-undo")
	public String clearUndoStackAndExit() {
		ndtService.clearUndoStack();
		return "redirect:/nhanvien/layout";
	}
	
	@PostMapping(value = "/investors/load-bank-ajax", consumes = "application/json", produces = "application/json")
	@ResponseBody
	public List<Map<String, Object>> loadBankAccountsAjax(@RequestBody Map<String, String> request) {
	    String maNDT = request.get("maNDT");

	    // Gọi service để lấy danh sách từ DB
	    List<TaiKhoanNganHang> accounts = taiKhoanNganHangService.findByInvestorMaNDT(maNDT);

	    // Chuyển danh sách sang JSON dạng đơn giản
	    return accounts.stream().map(acc -> {
	        Map<String, Object> map = new HashMap<>();
	        map.put("maTK", acc.getMaTK());
	        map.put("maNH", acc.getNganHang().getMaNH());
	        map.put("tenNH", acc.getNganHang().getTenNH());
	        map.put("soTien", acc.getSoTien());
	        return map;
	    }).collect(Collectors.toList());
	}

	@PostMapping(value = "/investors/load-nganhang-info", consumes = "application/json", produces = "application/json")
	@ResponseBody
	public Map<String, String> getNganHangInfo(@RequestBody Map<String, String> request) {
	    String maNH = request.get("maNH");
	    
	    Optional<NganHang> optional = nganHangService.findByMaNH(maNH);
	    if (optional.isEmpty()) {
	        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ngân hàng");
	    }

	    NganHang nh = optional.get();
	    Map<String, String> result = new HashMap<>();
	    result.put("maNH", nh.getMaNH());
	    result.put("tenNH", nh.getTenNH());
	    result.put("diaChi", nh.getDiaChi());
	    result.put("phone", nh.getPhone());
	    result.put("email", nh.getEmail());
	    return result;
	}

	@PostMapping("/investors/add-bank-account")
	@ResponseBody
	public ResponseEntity<?> addBankAccount(@RequestBody Map<String, String> request) {
	    try {
	        String maNDT = request.get("maNDT");
	        String maNH = request.get("maNH");
	        String maTK = request.get("maTK");
	        String soTienStr = request.get("soTien");

	        if (maNDT == null || maNH == null || maTK == null || soTienStr == null) {
	            return ResponseEntity.badRequest().body("Thiếu thông tin đầu vào");
	        }

	        BigDecimal soTien;
	        try {
	            soTien = new BigDecimal(soTienStr);
	            if (soTien.compareTo(BigDecimal.ZERO) < 0)
	                return ResponseEntity.badRequest().body("Số tiền không được âm");
	        } catch (NumberFormatException e) {
	            return ResponseEntity.badRequest().body("Số tiền không hợp lệ");
	        }

	        // Lấy NganHang từ service
	        Optional<NganHang> optionalNH = nganHangService.findByMaNH(maNH);
	        if (optionalNH.isEmpty())
	            return ResponseEntity.badRequest().body("Không tìm thấy ngân hàng");

	        // Lấy NhaDauTu từ service
	        NhaDauTu nhaDauTu = ndtRepository.findById(maNDT.trim()).orElse(null);

	        if (nhaDauTu == null)
	            return ResponseEntity.badRequest().body("Không tìm thấy nhà đầu tư");

	        // Tạo tài khoản ngân hàng
	        TaiKhoanNganHang tknh = TaiKhoanNganHang.builder()
	                .maTK(maTK)
	                .nhaDauTu(nhaDauTu)
	                .nganHang(optionalNH.get())
	                .soTien(soTien)
	                .build();

	        // Gọi service để xử lý thêm (dùng stored procedure)
	        taiKhoanNganHangService.themTaiKhoanNganHang(tknh);

	        return ResponseEntity.ok("Tạo tài khoản ngân hàng thành công");
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("Lỗi hệ thống: " + e.getMessage());
	    }
	}
	
	@PostMapping("/investors/edit-bank-temp")
    public String updateBankInfo(
            @RequestParam("maNDT") String maNDT,
            @RequestParam("nganHang.maNH") String maNH,
            @RequestParam("nganHang.tenNH") String tenNH,
            @RequestParam("nganHang.diaChi") String diaChi,
            @RequestParam("nganHang.phone") String phone,
            @RequestParam("nganHang.email") String email,
            RedirectAttributes redirectAttributes
    ) {
        boolean success = nganHangService.capNhatNganHang(maNH, tenNH, diaChi, phone, email);

        if (success) {
            redirectAttributes.addFlashAttribute("message", "Cập nhật ngân hàng thành công");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } else {
            redirectAttributes.addFlashAttribute("message", "Không tìm thấy ngân hàng để cập nhật");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/investors";
    }
}