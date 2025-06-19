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
import org.springframework.transaction.annotation.Transactional;
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

		Map<String, NhaDauTu> tempInvestorMap = tempList.stream()
				.filter(investor -> Objects.nonNull(investor.getMaNDT()) && !investor.getMaNDT().isEmpty())
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

		Map<String, List<TaiKhoanNganHang>> bankAccountMap = new HashMap<>();
		for (NhaDauTu investor : allInvestors) {
			String maNDT = investor.getMaNDT();
			if (tempBankAccounts.containsKey(maNDT)) {
				bankAccountMap.put(maNDT, new ArrayList<>(tempBankAccounts.get(maNDT)));
			} else {
				bankAccountMap.put(maNDT, taiKhoanNganHangService.findByInvestorMaNDT(maNDT));
			}
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
				.filter(Objects::nonNull)
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

		return "nhanvien/investor_list";
	}

	@PostMapping("/investors/edit-temp")
	public String editTempInvestor(@ModelAttribute NhaDauTu investor,
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

		// Kiểm tra tính hợp lệ của MaNDT
		if (Objects.isNull(investor.getMaNDT()) || investor.getMaNDT().isEmpty()) {
			ra.addFlashAttribute("message", "Mã NDT không hợp lệ!");
			ra.addFlashAttribute("messageType", "error");
			return "redirect:/investors?page=" + page + "&size=" + size;
		}

		// Kiểm tra tài khoản ngân hàng đang sử dụng trong Lệnh Đặt
		List<TaiKhoanNganHang> usedAccounts = taiKhoanNganHangService.findTaiKhoanNganHangInLenhDat();
		Set<String> usedMaTKs = usedAccounts.stream()
				.map(TaiKhoanNganHang::getMaTK)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		// Cập nhật hoặc thêm nhà đầu tư vào tempList
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

		// Xử lý danh sách tài khoản ngân hàng
		List<TaiKhoanNganHang> validAccounts = new ArrayList<>();
		if (investor.getTaiKhoanNganHangs() != null) {
			for (TaiKhoanNganHang tknh : investor.getTaiKhoanNganHangs()) {
				if (Objects.nonNull(tknh.getMaTK()) && !tknh.getMaTK().isEmpty() &&
						Objects.nonNull(tknh.getNganHang()) && Objects.nonNull(tknh.getNganHang().getMaNH()) &&
						!tknh.getNganHang().getMaNH().isEmpty()) {
					if (usedMaTKs.contains(tknh.getMaTK())) {
						continue;
					}
					validAccounts.add(tknh);
				} else {
				}
			}
		}

		tempBankAccounts.put(investor.getMaNDT(), validAccounts);
		session.setAttribute("temporaryInvestors", tempList);
		session.setAttribute("tempBankAccounts", tempBankAccounts);

		ra.addFlashAttribute("message", "Đã cập nhật tạm nhà đầu tư " + investor.getMaNDT() + ". Một số tài khoản có thể bị bỏ qua do đang được sử dụng.");
		ra.addFlashAttribute("messageType", "success");


		return "redirect:/investors?page=" + page + "&size=" + size;
	}

	@PostMapping("/investors/delete-bank-account")
	public String deleteBankAccount(@RequestParam String maTK,
									@RequestParam String maNDT,
									@RequestParam(defaultValue = "0") int page,
									@RequestParam(defaultValue = "5") int size,
									HttpSession session,
									RedirectAttributes ra) {
		try {
			// Kiểm tra tài khoản có đang được sử dụng trong Lệnh Đặt
			List<TaiKhoanNganHang> usedAccounts = taiKhoanNganHangService.findTaiKhoanNganHangInLenhDat();
			Set<String> usedMaTKs = usedAccounts.stream()
					.map(TaiKhoanNganHang::getMaTK)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			if (usedMaTKs.contains(maTK)) {
				ra.addFlashAttribute("message", "Đã bỏ qua xóa tài khoản ngân hàng " + maTK + " vì đang được sử dụng!");
				ra.addFlashAttribute("messageType", "warning");
			} else {

				@SuppressWarnings("unchecked")
				Map<String, List<TaiKhoanNganHang>> tempBankAccounts = (Map<String, List<TaiKhoanNganHang>>) session.getAttribute("tempBankAccounts");
				if (tempBankAccounts != null && tempBankAccounts.containsKey(maNDT)) {
					tempBankAccounts.get(maNDT).removeIf(acc -> acc.getMaTK().equals(maTK));
					if (tempBankAccounts.get(maNDT).isEmpty()) {
						tempBankAccounts.remove(maNDT);
					}
					session.setAttribute("tempBankAccounts", tempBankAccounts);
					ra.addFlashAttribute("message", "Đã xóa tạm tài khoản ngân hàng " + maTK);
					ra.addFlashAttribute("messageType", "success");
				} else {
					ra.addFlashAttribute("message", "Không tìm thấy tài khoản ngân hàng " + maTK + " trong danh sách tạm!");
					ra.addFlashAttribute("messageType", "warning");
				}
			}

		} catch (Exception e) {
			ra.addFlashAttribute("message", "Lỗi khi xóa tài khoản ngân hàng: " + e.getMessage());
			ra.addFlashAttribute("messageType", "error");
		}

		return "redirect:/investors?page=" + page + "&size=" + size;
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

		if (Objects.isNull(investor.getMaNDT()) || investor.getMaNDT().isEmpty()) {
			ra.addFlashAttribute("message", "Mã NDT không hợp lệ!");
			ra.addFlashAttribute("messageType", "error");
			return "redirect:/investors?page=" + page + "&size=" + size;
		}

		boolean exists = tempList.stream().anyMatch(i -> i.getMaNDT().equals(investor.getMaNDT()));
		if (exists) {
			ra.addFlashAttribute("message", "Mã NDT " + investor.getMaNDT() + " đã tồn tại trong danh sách tạm!");
			ra.addFlashAttribute("messageType", "error");
		} else {
			// Kiểm tra tài khoản ngân hàng đang sử dụng trong Lệnh Đặt
			List<TaiKhoanNganHang> usedAccounts = taiKhoanNganHangService.findTaiKhoanNganHangInLenhDat();
			Set<String> usedMaTKs = usedAccounts.stream()
					.map(TaiKhoanNganHang::getMaTK)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			List<TaiKhoanNganHang> validAccounts = new ArrayList<>();
			if (investor.getTaiKhoanNganHangs() != null) {
				for (TaiKhoanNganHang tknh : investor.getTaiKhoanNganHangs()) {
					if (Objects.nonNull(tknh.getMaTK()) && !tknh.getMaTK().isEmpty() &&
							Objects.nonNull(tknh.getNganHang()) && Objects.nonNull(tknh.getNganHang().getMaNH()) &&
							!tknh.getNganHang().getMaNH().isEmpty()) {
						if (usedMaTKs.contains(tknh.getMaTK())) {
							continue;
						}
						validAccounts.add(tknh);
					} else {

					}
				}
			}

			tempList.add(investor);
			if (!validAccounts.isEmpty()) {
				tempBankAccounts.put(investor.getMaNDT(), new ArrayList<>(validAccounts));
			}
			session.setAttribute("temporaryInvestors", tempList);
			session.setAttribute("tempBankAccounts", tempBankAccounts);
			ra.addFlashAttribute("message", "Đã thêm tạm nhà đầu tư " + investor.getMaNDT() + ". Một số tài khoản có thể bị bỏ qua do đang được sử dụng.");
			ra.addFlashAttribute("messageType", "success");
		}
		return "redirect:/investors?page=" + page + "&size=" + size;
	}

	@PostMapping("/investors/save-all")
	@Transactional
	public String saveAllInvestors(HttpSession session, RedirectAttributes ra) {
		@SuppressWarnings("unchecked")
		List<NhaDauTu> tempList = (List<NhaDauTu>) session.getAttribute("temporaryInvestors");
		@SuppressWarnings("unchecked")
		Map<String, List<TaiKhoanNganHang>> tempBankAccounts = (Map<String, List<TaiKhoanNganHang>>) session.getAttribute("tempBankAccounts");

		if (tempList == null || tempList.isEmpty()) {
			ra.addFlashAttribute("message", "Không có nhà đầu tư tạm để lưu!");
			ra.addFlashAttribute("messageType", "error");
			return "redirect:/investors";
		}

		try {
			List<TaiKhoanNganHang> usedAccounts = taiKhoanNganHangService.findTaiKhoanNganHangInLenhDat();
			Set<String> usedMaTKs = usedAccounts.stream()
					.map(TaiKhoanNganHang::getMaTK)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			for (NhaDauTu investor : tempList) {
				if (Objects.isNull(investor.getMaNDT()) || investor.getMaNDT().isEmpty()) {
					continue;
				}

				// Lấy danh sách tài khoản hiện tại và tạm thời
				List<TaiKhoanNganHang> existingBankAccounts = taiKhoanNganHangService.findByInvestorMaNDT(investor.getMaNDT());
				List<TaiKhoanNganHang> tempAccounts = tempBankAccounts != null ? tempBankAccounts.getOrDefault(investor.getMaNDT(), new ArrayList<>()) : new ArrayList<>();

				// Tạo map để so sánh
				Map<String, TaiKhoanNganHang> existingAccountsMap = existingBankAccounts.stream()
						.filter(acc -> Objects.nonNull(acc.getMaTK()))
						.collect(Collectors.toMap(TaiKhoanNganHang::getMaTK, acc -> acc, (a1, a2) -> a1));
				Map<String, TaiKhoanNganHang> tempAccountsMap = tempAccounts.stream()
						.filter(acc -> Objects.nonNull(acc.getMaTK()))
						.collect(Collectors.toMap(TaiKhoanNganHang::getMaTK, acc -> acc, (a1, a2) -> a1));

				// Xóa các tài khoản không còn trong danh sách tạm
				for (TaiKhoanNganHang existingAcc : existingBankAccounts) {
					if (!tempAccountsMap.containsKey(existingAcc.getMaTK())) {
						if (usedMaTKs.contains(existingAcc.getMaTK())) {
							continue;
						}
						taiKhoanNganHangService.deleteTaiKhoanNganHang(existingAcc.getMaTK());
					}
				}

				// Thêm hoặc cập nhật tài khoản ngân hàng
				for (TaiKhoanNganHang tempAcc : tempAccounts) {
					if (Objects.isNull(tempAcc.getMaTK()) || tempAcc.getMaTK().isEmpty() ||
							Objects.isNull(tempAcc.getNganHang()) || Objects.isNull(tempAcc.getNganHang().getMaNH()) ||
							tempAcc.getNganHang().getMaNH().isEmpty()) {
						continue;
					}

					if (usedMaTKs.contains(tempAcc.getMaTK())) {
						continue;
					}

					tempAcc.setNhaDauTu(investor);
					if (!nganHangService.existsByMaNH(tempAcc.getNganHang().getMaNH())) {
						nganHangService.themNganHang(tempAcc.getNganHang());
					}

					if (existingAccountsMap.containsKey(tempAcc.getMaTK())) {
						taiKhoanNganHangService.capNhatHoacThemTaiKhoanNganHang(tempAcc);
					} else {
						taiKhoanNganHangService.themTaiKhoanNganHang(tempAcc);
					}
				}

				// Lưu hoặc cập nhật nhà đầu tư
				Optional<NhaDauTu> existing = nhaDauTuService.findById(investor.getMaNDT());
				if (existing.isPresent()) {
					nhaDauTuService.capNhatNhaDauTu(investor.getMaNDT(), investor);
				} else {
					nhaDauTuService.themNhaDauTuBangSP(investor);
				}
			}

			// Xóa dữ liệu tạm sau khi lưu thành công
			session.removeAttribute("temporaryInvestors");
			session.removeAttribute("tempBankAccounts");
			ra.addFlashAttribute("message", "Đã lưu tất cả nhà đầu tư và tài khoản ngân hàng! Một số tài khoản có thể được bỏ qua do đang được sử dụng.");
			ra.addFlashAttribute("messageType", "success");

		} catch (Exception e) {
			ra.addFlashAttribute("message", "Lỗi khi lưu dữ liệu: " + e.getMessage());
			ra.addFlashAttribute("messageType", "error");
			throw new RuntimeException("Lỗi khi lưu dữ liệu", e);
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
			ra.addFlashAttribute("message", "Xóa thất bại: " + e.getMessage());
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
			tempList.removeIf(investor -> Objects.equals(investor.getMaNDT(), maNDT));
			if (tempBankAccounts != null) {
				tempBankAccounts.remove(maNDT);
			}
			session.setAttribute("temporaryInvestors", tempList);
			session.setAttribute("tempBankAccounts", tempBankAccounts);
			ra.addFlashAttribute("message", "Đã xóa nhà đầu tư tạm có mã " + maNDT + " và tài khoản ngân hàng liên quan");
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

		List<TaiKhoanNganHang> usedAccounts = taiKhoanNganHangService.findTaiKhoanNganHangInLenhDat();
		Set<String> usedMaTKs = usedAccounts.stream()
				.map(TaiKhoanNganHang::getMaTK)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		model.addAttribute("usedMaTKs", usedMaTKs);

		Map<String, List<TaiKhoanNganHang>> bankAccountMap = new HashMap<>();
		@SuppressWarnings("unchecked")
		Map<String, List<TaiKhoanNganHang>> tempBankAccounts = (Map<String, List<TaiKhoanNganHang>>) session.getAttribute("tempBankAccounts");
		if (tempBankAccounts == null) {
			tempBankAccounts = new HashMap<>();
		}
		for (NhaDauTu investor : allInvestors) {
			String maNDT = investor.getMaNDT();
			if (tempBankAccounts.containsKey(maNDT)) {
				bankAccountMap.put(maNDT, new ArrayList<>(tempBankAccounts.get(maNDT)));
			} else {
				bankAccountMap.put(maNDT, taiKhoanNganHangService.findByInvestorMaNDT(maNDT));
			}
		}

		model.addAttribute("investors", investorPage);
		model.addAttribute("temporaryInvestors", tempList);
		model.addAttribute("bankAccountMap", bankAccountMap);
		model.addAttribute("canUndo", !nhaDauTuService.isUndoStackEmpty());

		if (allInvestors.isEmpty()) {
			model.addAttribute("message", "Không tìm thấy nhà đầu tư nào.");
			model.addAttribute("messageType", "danger");
		}

		return "nhanvien/investor_list";
	}

}