package chungkhoan.controller;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class InvestorsController {

	@Autowired
	private NDTService nhaDauTuService;

	@GetMapping("/investors")
	public String listInvestors(@RequestParam(defaultValue = "0") int page,
								@RequestParam(defaultValue = "5") int size,
								Model model,
								HttpSession session) {
		// Lấy danh sách nhà đầu tư chính từ cơ sở dữ liệu
		Page<NhaDauTu> investorPage = nhaDauTuService.getPaginated(0, Integer.MAX_VALUE);

		// Lấy danh sách nhà đầu tư tạm từ session
		@SuppressWarnings("unchecked")
		List<NhaDauTu> tempList = (List<NhaDauTu>) session.getAttribute("temporaryInvestors");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		// Kết hợp danh sách: danh sách chính trước, danh sách tạm sau
		List<NhaDauTu> allInvestors = new ArrayList<>();
		allInvestors.addAll(investorPage.getContent());
		allInvestors.addAll(tempList);

		// Tính toán phân trang cho danh sách kết hợp
		int totalItems = allInvestors.size();
		int start = page * size;
		int end = Math.min(start + size, totalItems);

		// Đảm bảo start và end hợp lệ
		if (start >= totalItems && totalItems > 0) {
			page = (totalItems - 1) / size;
			start = page * size;
			end = Math.min(start + size, totalItems);
		} else if (start >= totalItems) {
			start = 0;
			end = 0;
		}

		List<NhaDauTu> allInvestorsPageContent = (start < end) ? allInvestors.subList(start, end) : new ArrayList<>();

		// Tạo đối tượng Page cho danh sách kết hợp
		Page<NhaDauTu> allInvestorsPage = new PageImpl<>(allInvestorsPageContent, PageRequest.of(page, size), totalItems);

		// Thêm vào model
		model.addAttribute("investors", allInvestorsPage);
		model.addAttribute("temporaryInvestors", tempList);
		if (allInvestorsPage.isEmpty()) {
			model.addAttribute("message", "Không có dữ liệu nhà đầu tư.");
			model.addAttribute("messageType", "danger");
		}

		// Kiểm tra nếu có thể hoàn tác
		model.addAttribute("canUndo", !nhaDauTuService.isUndoStackEmpty());

		// Thêm logging để kiểm tra
		System.out.println("TempList size: " + tempList.size());
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

		// Tạo mã nhà đầu tư ngẫu nhiên hoặc dựa trên logic (ở đây giả định tự sinh)
		if (investor.getMaNDT() == null || investor.getMaNDT().isEmpty()) {
			investor.setMaNDT("NDT" + System.currentTimeMillis()); // Ví dụ tạo mã tạm
		}

		// Kiểm tra xem mã NDT đã tồn tại trong danh sách tạm chưa
		boolean exists = tempList.stream().anyMatch(i -> i.getMaNDT().equals(investor.getMaNDT()));
		if (exists) {
			ra.addFlashAttribute("message", "Mã NDT " + investor.getMaNDT() + " đã tồn tại trong danh sách tạm!");
			ra.addFlashAttribute("messageType", "error");
		} else {
			tempList.add(investor);
			session.setAttribute("temporaryInvestors", tempList);
			ra.addFlashAttribute("message", "Đã thêm tạm nhà đầu tư " + investor.getMaNDT());
			ra.addFlashAttribute("messageType", "success");

			// Thêm logging để kiểm tra
			System.out.println("Added investor to tempList: " + investor.getMaNDT());
			System.out.println("TempList after adding: " + tempList);
		}

		// Chuyển hướng về trang hiện tại
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
		ra.addFlashAttribute("message", "Đã cập nhật tạm nhà đầu tư " + investor.getMaNDT());
		ra.addFlashAttribute("messageType", "success");

		return "redirect:/investors";
	}

	@PostMapping("/investors/save-all")
	public String saveAllInvestors(HttpSession session, RedirectAttributes ra) {
		@SuppressWarnings("unchecked")
		List<NhaDauTu> tempList = (List<NhaDauTu>) session.getAttribute("temporaryInvestors");
		if (tempList != null && !tempList.isEmpty()) {
			for (NhaDauTu investor : tempList) {
				Optional<NhaDauTu> existing = nhaDauTuService.findById(investor.getMaNDT());
				if (existing.isPresent()) {
					nhaDauTuService.capNhatNhaDauTu(investor.getMaNDT(), investor);
				} else {
					nhaDauTuService.themNhaDauTuBangSP(investor);
				}
			}
			session.removeAttribute("temporaryInvestors");
			ra.addFlashAttribute("message", "Đã ghi vào cơ sở dữ liệu!");
			ra.addFlashAttribute("messageType", "success");
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
		if (tempList != null) {
			tempList.removeIf(investor -> investor.getMaNDT().equals(maNDT));
			session.setAttribute("temporaryInvestors", tempList);
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
		// Tìm kiếm trong danh sách chính
		List<NhaDauTu> searchResults = nhaDauTuService.searchInvestors(query);

		// Lấy danh sách tạm từ session
		@SuppressWarnings("unchecked")
		List<NhaDauTu> tempList = (List<NhaDauTu>) session.getAttribute("temporaryInvestors");
		if (tempList == null) {
			tempList = new ArrayList<>();
		}

		// Kết hợp kết quả tìm kiếm với danh sách tạm
		List<NhaDauTu> allInvestors = new ArrayList<>(searchResults);
		allInvestors.addAll(tempList);

		// Tạo trang dữ liệu
		Page<NhaDauTu> investorPage = new PageImpl<>(allInvestors, PageRequest.of(0, Integer.MAX_VALUE), allInvestors.size());

		// Thêm vào model
		model.addAttribute("investors", investorPage);
		model.addAttribute("temporaryInvestors", tempList);
		model.addAttribute("canUndo", !nhaDauTuService.isUndoStackEmpty());

		if (allInvestors.isEmpty()) {
			model.addAttribute("message", "Không tìm thấy nhà đầu tư nào.");
			model.addAttribute("messageType", "danger");
		}

		return "nhanvien/investors";
	}
}