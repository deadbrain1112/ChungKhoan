package chungkhoan.controller;

import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.service.NganHangService;
import chungkhoan.service.TaiKhoanNganHangService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.service.NDTService;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class InvestorsController {

	@Autowired
	private NDTService nhaDauTuService;
	@Autowired
	private TaiKhoanNganHangService taiKhoanNganHangService;
	@Autowired
	private NganHangService nganHangService;

	@GetMapping("/investors")
	public String listInvestors(@RequestParam(defaultValue = "0") int page,
								@RequestParam(defaultValue = "5") int size,
								Model model,
								HttpSession session) {
		Page<NhaDauTu> investorPage = nhaDauTuService.getPaginated(page, size);

		@SuppressWarnings("unchecked")
		List<NhaDauTu> tempList = (List<NhaDauTu>) session.getAttribute("temporaryInvestors");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		@SuppressWarnings("unchecked")
		Map<String, List<TaiKhoanNganHang>> tempBankAccounts = (Map<String, List<TaiKhoanNganHang>>) session.getAttribute("tempBankAccounts");
		if (tempBankAccounts == null) {
			tempBankAccounts = new HashMap<>();
		}

		Map<String, NhaDauTu> tempInvestorMap = tempList.isEmpty()
				? Collections.emptyMap()
				: tempList.stream()
				.filter(investor -> investor.getMaNDT() != null)
				.collect(Collectors.toMap(
						NhaDauTu::getMaNDT,
						investor -> investor,
						(existing, replacement) -> replacement
				));

		List<NhaDauTu> allInvestors = new ArrayList<>();
		for (NhaDauTu investor : investorPage.getContent()) {
			if (!tempInvestorMap.containsKey(investor.getMaNDT())) {
				allInvestors.add(investor);
			}
		}
		allInvestors.addAll(tempList);

		Map<String, List<TaiKhoanNganHang>> bankAccountMap = taiKhoanNganHangService.getBankAccountsForInvestors(allInvestors);
		for (String maNDT : tempBankAccounts.keySet()) {
			bankAccountMap.put(maNDT, tempBankAccounts.get(maNDT));
		}

		int totalItems = allInvestors.size();
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

		List<NhaDauTu> allInvestorsPageContent = (start < end) ? allInvestors.subList(start, end) : new ArrayList<>();
		Page<NhaDauTu> allInvestorsPage = new PageImpl<>(allInvestorsPageContent, PageRequest.of(page, size), totalItems);
		
		List<TaiKhoanNganHang> usedAccounts = taiKhoanNganHangService.findTaiKhoanNganHangInLenhDat();
		Set<String> usedMaTKs = usedAccounts.stream()
	            .map(TaiKhoanNganHang::getMaTK)
	            .collect(Collectors.toSet());
		model.addAttribute("usedMaTKs", usedMaTKs);
		
		model.addAttribute("investors", allInvestorsPage);
		model.addAttribute("temporaryInvestors", tempList);
		model.addAttribute("tempInvestorMap", tempInvestorMap);
		model.addAttribute("bankAccountMap", bankAccountMap);
		if (allInvestorsPage.isEmpty()) {
			model.addAttribute("message", "Không có dữ liệu nhà đầu tư.");
			model.addAttribute("messageType", "danger");
		}

		model.addAttribute("canUndo", !nhaDauTuService.isUndoStackEmpty());

		System.out.println("TempList size: " + tempList.size());
		System.out.println("Temp investor map: " + tempInvestorMap);
		System.out.println("Temp bank accounts: " + tempBankAccounts);
		System.out.println("AllInvestors size: " + allInvestors.size());
		System.out.println("Page content size: " + allInvestorsPageContent.size());

		return "nhanvien/investor_list";
	}

	@PostMapping("/investors/add-temp")
	public String addTempInvestor(@ModelAttribute NhaDauTu investor,
								  @RequestParam(defaultValue = "0") int page,
								  @RequestParam(defaultValue = "5") int size,
								  HttpSession session,
								  RedirectAttributes ra) {
		@SuppressWarnings("unchecked")
		List<NhaDauTu> tempList = (List<NhaDauTu>) session.getAttribute("temporaryInvestors");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		@SuppressWarnings("unchecked")
		Map<String, List<TaiKhoanNganHang>> tempBankAccounts = (Map<String, List<TaiKhoanNganHang>>) session.getAttribute("tempBankAccounts");
		if (tempBankAccounts == null) {
			tempBankAccounts = new HashMap<>();
		}

		System.out.println("Received investor from form (add): " + investor);
		System.out.println("Bank accounts from form (add): " + investor.getTaiKhoanNganHangs());

		boolean exists = tempList.stream().anyMatch(i -> i.getMaNDT().equals(investor.getMaNDT()));
		if (exists) {
			ra.addFlashAttribute("message", "Mã NDT " + investor.getMaNDT() + " đã tồn tại trong danh sách tạm!");
			ra.addFlashAttribute("messageType", "error");
		} else {
			tempList.add(investor);
			if (investor.getTaiKhoanNganHangs() != null && !investor.getTaiKhoanNganHangs().isEmpty()) {
				tempBankAccounts.put(investor.getMaNDT(), new ArrayList<>(investor.getTaiKhoanNganHangs()));
			}
			session.setAttribute("temporaryInvestors", tempList);
			session.setAttribute("tempBankAccounts", tempBankAccounts);
			ra.addFlashAttribute("message", "Đã thêm tạm nhà đầu tư " + investor.getMaNDT());
			ra.addFlashAttribute("messageType", "success");
		}

		System.out.println("TempList after adding: " + tempList);
		System.out.println("Temp bank accounts after adding: " + tempBankAccounts);

		return "redirect:/investors?page=" + page + "&size=" + size;
	}

	@PostMapping("/investors/edit-temp")
	public String editTempInvestor(@ModelAttribute NhaDauTu investor,
								   HttpSession session,
								   RedirectAttributes ra) {
		@SuppressWarnings("unchecked")
		List<NhaDauTu> tempList = (List<NhaDauTu>) session.getAttribute("temporaryInvestors");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		@SuppressWarnings("unchecked")
		Map<String, List<TaiKhoanNganHang>> tempBankAccounts = (Map<String, List<TaiKhoanNganHang>>) session.getAttribute("tempBankAccounts");
		if (tempBankAccounts == null) {
			tempBankAccounts = new HashMap<>();
		}

		System.out.println("Received investor from form (edit): " + investor);
		System.out.println("Bank accounts from form (edit): " + investor.getTaiKhoanNganHangs());

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

		if (investor.getTaiKhoanNganHangs() != null) {
			tempBankAccounts.put(investor.getMaNDT(), new ArrayList<>(investor.getTaiKhoanNganHangs()));
		}

		System.out.println("Edited investor: " + investor.getMaNDT());
		System.out.println("Temp bank accounts after edit: " + tempBankAccounts);
		System.out.println("TempList after edit: " + tempList);

		session.setAttribute("temporaryInvestors", tempList);
		session.setAttribute("tempBankAccounts", tempBankAccounts);
		ra.addFlashAttribute("message", "Đã cập nhật tạm nhà đầu tư " + investor.getMaNDT());
		ra.addFlashAttribute("messageType", "success");

		return "redirect:/investors";
	}

	@PostMapping("/investors/save-all")
	public String saveAllInvestors(HttpSession session, RedirectAttributes ra) {
		@SuppressWarnings("unchecked")
		List<NhaDauTu> tempList = (List<NhaDauTu>) session.getAttribute("temporaryInvestors");
		@SuppressWarnings("unchecked")
		Map<String, List<TaiKhoanNganHang>> tempBankAccounts = (Map<String, List<TaiKhoanNganHang>>) session.getAttribute("tempBankAccounts");

		if (tempList != null && !tempList.isEmpty()) {
			try {
				for (NhaDauTu investor : tempList) {
					if (tempBankAccounts != null && tempBankAccounts.containsKey(investor.getMaNDT())) {
						List<TaiKhoanNganHang> bankAccounts = tempBankAccounts.get(investor.getMaNDT());
						investor.setTaiKhoanNganHangs(bankAccounts);

						for (TaiKhoanNganHang tknh : bankAccounts) {
							tknh.setNhaDauTu(investor);
							System.out.println("Processing TaiKhoanNganHang: MaTK=" + tknh.getMaTK() + ", MaNH=" + (tknh.getNganHang() != null ? tknh.getNganHang().getMaNH() : "null"));
						}
					}

					Optional<NhaDauTu> existing = nhaDauTuService.findById(investor.getMaNDT());
					if (existing.isPresent()) {
						// Cập nhật thông tin nhà đầu tư
						nhaDauTuService.capNhatNhaDauTu(investor.getMaNDT(), investor);
						taiKhoanNganHangService.deleteByInvestorMaNDT(investor.getMaNDT());

						// Kiểm tra và thêm ngân hàng mới nếu cần trước khi lưu tài khoản ngân hàng
						if (investor.getTaiKhoanNganHangs() != null && !investor.getTaiKhoanNganHangs().isEmpty()) {
							for (TaiKhoanNganHang tknh : investor.getTaiKhoanNganHangs()) {
								if (tknh.getNganHang() != null && tknh.getNganHang().getMaNH() != null) {

									System.out.println("Checking NganHang with MaNH: " + tknh.getNganHang().getMaNH());

									nganHangService.themNganHang(tknh.getNganHang());
								}
							}

							// Sau đó lưu các tài khoản ngân hàng
							for (TaiKhoanNganHang tknh : investor.getTaiKhoanNganHangs()) {
								taiKhoanNganHangService.themTaiKhoanNganHang(tknh);
							}
						}
					} else {
						// Thêm mới nhà đầu tư
						nhaDauTuService.themNhaDauTuBangSP(investor);

						// Lưu thông tin ngân hàng trước
						if (investor.getTaiKhoanNganHangs() != null && !investor.getTaiKhoanNganHangs().isEmpty()) {
							for (TaiKhoanNganHang tknh : investor.getTaiKhoanNganHangs()) {
								nganHangService.themNganHang(tknh.getNganHang());
							}

							// Sau đó lưu tài khoản ngân hàng
							for (TaiKhoanNganHang tknh : investor.getTaiKhoanNganHangs()) {
								taiKhoanNganHangService.themTaiKhoanNganHang(tknh);
							}
						}
					}
				}

				session.removeAttribute("temporaryInvestors");
				session.removeAttribute("tempBankAccounts");
				ra.addFlashAttribute("message", "Đã ghi vào cơ sở dữ liệu!");
				ra.addFlashAttribute("messageType", "success");
			} catch (Exception e) {
				System.out.println("Error in saveAllInvestors: " + e.getMessage());
				ra.addFlashAttribute("message", "Lỗi khi ghi vào cơ sở dữ liệu: " + e.getMessage());
				ra.addFlashAttribute("messageType", "error");
			}
		} else {
			ra.addFlashAttribute("message", "Không có nhà đầu tư tạm để ghi.");
			ra.addFlashAttribute("messageType", "error");
		}

		return "redirect:/investors";
	}
	@PostMapping("/investors/delete")
	public String deleteInvestor(@RequestParam String maNDT, RedirectAttributes ra) {
		try {
			nhaDauTuService.xoaNhaDauTu(maNDT);
			ra.addFlashAttribute("message", "Đã xóa nhà đầu tư " + maNDT);
			ra.addFlashAttribute("messageType", "success");
		} catch (Exception e) {
			ra.addFlashAttribute("message", "Xóa thất bại");
			ra.addFlashAttribute("messageType", "error");
		}
		return "redirect:/investors";
	}

	@PostMapping("/investors/remove-temp")
	public String removeTempInvestor(@RequestParam String maNDT, HttpSession session, RedirectAttributes ra) {
		@SuppressWarnings("unchecked")
		List<NhaDauTu> tempList = (List<NhaDauTu>) session.getAttribute("temporaryInvestors");
		@SuppressWarnings("unchecked")
		Map<String, List<TaiKhoanNganHang>> tempBankAccounts = (Map<String, List<TaiKhoanNganHang>>) session.getAttribute("tempBankAccounts");

		if (tempList != null) {
			tempList.removeIf(investor -> investor.getMaNDT().equals(maNDT));
			if (tempBankAccounts != null) {
				tempBankAccounts.remove(maNDT);
			}
			session.setAttribute("temporaryInvestors", tempList);
			session.setAttribute("tempBankAccounts", tempBankAccounts);
			ra.addFlashAttribute("message", "Đã xóa nhà đầu tư tạm có mã " + maNDT);
			ra.addFlashAttribute("messageType", "success");
		} else {
			ra.addFlashAttribute("message", "Không tìm thấy nhà đầu tư tạm nào để xóa.");
			ra.addFlashAttribute("messageType", "error");
		}
		return "redirect:/investors";
	}

	@GetMapping("/investors/reload")
	public String reloadInvestors(HttpSession session) {
		session.removeAttribute("temporaryInvestors");
		session.removeAttribute("tempBankAccounts");
		return "redirect:/investors";
	}

	@PostMapping("/investors/undo")
	public String undo(RedirectAttributes ra) {
		boolean ok = nhaDauTuService.undoThaoTacCuoi();
		if (ok) {
			ra.addFlashAttribute("message", "Hoàn tác thành công");
			ra.addFlashAttribute("messageType", "success");
		} else {
			ra.addFlashAttribute("message", "Không có thao tác để hoàn tác");
			ra.addFlashAttribute("messageType", "error");
		}
		return "redirect:/investors";
	}

	@PostMapping("/investors/clear-undo")
	public String clearUndo() {
		nhaDauTuService.clearUndoStack();
		return "redirect:/nhanvien/layout";
	}

	@PostMapping("/investors/search")
	public String searchInvestors(@RequestParam String query, Model model, HttpSession session) {
		List<NhaDauTu> searchResults = nhaDauTuService.searchInvestors(query);

		@SuppressWarnings("unchecked")
		List<NhaDauTu> tempList = (List<NhaDauTu>) session.getAttribute("temporaryInvestors");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		List<NhaDauTu> allInvestors = new ArrayList<>(searchResults);
		allInvestors.addAll(tempList);

		Page<NhaDauTu> investorPage = new PageImpl<>(allInvestors, PageRequest.of(0, Integer.MAX_VALUE), allInvestors.size());

		model.addAttribute("investors", investorPage);
		model.addAttribute("temporaryInvestors", tempList);
		model.addAttribute("canUndo", !nhaDauTuService.isUndoStackEmpty());

		if (allInvestors.isEmpty()) {
			model.addAttribute("message", "Không tìm thấy nhà đầu tư nào.");
			model.addAttribute("messageType", "danger");
		}

		return "nhanvien/investor_list";
	}
}