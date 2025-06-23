package chungkhoan.controller;

import chungkhoan.dto.NganHangTemp;
import chungkhoan.entity.NganHang;
import chungkhoan.repository.TaiKhoanNganHangRepository;
import chungkhoan.service.NganHangService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class BankController {

    @Autowired
    private NganHangService nganHangService;
    
    @Autowired
    private TaiKhoanNganHangRepository taiKhoanNganHangRepository;

    @GetMapping("/banks")
    public String bankList(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "5") int size,
                           Model model,
                           HttpSession session) {

        List<NganHang> fromDb = nganHangService.getPaginated(0, Integer.MAX_VALUE).getContent();

        @SuppressWarnings("unchecked")
        List<NganHangTemp> tempList = (List<NganHangTemp>) session.getAttribute("temporaryBanks");
        if (tempList == null) {
            tempList = new ArrayList<>();
        }

        // Lọc các ngân hàng từ DB không trùng mã với danh sách tạm
        List<NganHangTemp> finalTempList = tempList;
        List<NganHang> filteredDb = fromDb.stream()
                .filter(nh -> finalTempList.stream().noneMatch(t -> t.getMaNH().equals(nh.getMaNH())))
                .toList();

        // Chuyển tất cả ngân hàng từ DB sang Temp
        List<NganHangTemp> allBanks = new ArrayList<>();
        for (NganHang nh : filteredDb) {
            allBanks.add(new NganHangTemp(nh));
        }
        allBanks.addAll(tempList);

        // Phân trang
        int totalItems = allBanks.size();
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

        List<NganHangTemp> pageContent = (start < end) ? allBanks.subList(start, end) : new ArrayList<>();
        Page<NganHangTemp> allBanksPage = new PageImpl<>(pageContent, PageRequest.of(page, size), totalItems);

        // Gửi dữ liệu ra view
        model.addAttribute("banks", allBanksPage);
        model.addAttribute("temporaryBanks", tempList);
        model.addAttribute("canUndo", !nganHangService.isUndoStackEmpty());

        if (allBanksPage.isEmpty()) {
            model.addAttribute("message", "Không có dữ liệu ngân hàng.");
            model.addAttribute("messageType", "danger");
        }

        List<String> temporaryMaNHList = tempList.stream()
                .filter(t -> !t.isDaXoa())
                .map(NganHangTemp::getMaNH)
                .toList();
        model.addAttribute("temporaryMaNHList", temporaryMaNHList);

        return "nhanvien/bank_list";
    }

    @PostMapping("/banks/add-temp")
    public String addTempBank(@ModelAttribute NganHangTemp bank,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "5") int size,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        @SuppressWarnings("unchecked")
        List<NganHangTemp> tempList = (List<NganHangTemp>) session.getAttribute("temporaryBanks");
        if (tempList == null) tempList = new ArrayList<>();

        // Kiểm tra trùng mã ngân hàng
        boolean maNHTrung = nganHangService.existsById(bank.getMaNH()) ||
                tempList.stream()
                        .filter(temp -> !temp.isDaXoa())
                        .anyMatch(temp -> temp.getMaNH().equals(bank.getMaNH()));
        if (maNHTrung) {
            redirectAttributes.addFlashAttribute("message", "Mã ngân hàng " + bank.getMaNH() + " đã tồn tại!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/banks?page=" + page + "&size=" + size;
        }

        // Kiểm tra email hợp lệ
        if (bank.getEmail() != null && !bank.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            redirectAttributes.addFlashAttribute("message", "Email không hợp lệ!");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/banks?page=" + page + "&size=" + size;
        }

        // Thêm vào danh sách tạm
        tempList.add(bank);
        session.setAttribute("temporaryBanks", tempList);
        redirectAttributes.addFlashAttribute("message", "Đã thêm tạm ngân hàng " + bank.getMaNH());
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/banks?page=" + page + "&size=" + size;
    }

    @PostMapping("/banks/edit-temp")
    public String editTempBank(@ModelAttribute NganHangTemp bank,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        @SuppressWarnings("unchecked")
        List<NganHangTemp> tempList = (List<NganHangTemp>) session.getAttribute("temporaryBanks");
        if (tempList == null) {
            tempList = new ArrayList<>();
        }

        // Kiểm tra email hợp lệ
        if (bank.getEmail() != null && !bank.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            redirectAttributes.addFlashAttribute("message", "Email không hợp lệ!");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/banks";
        }

        // Cập nhật hoặc thêm vào danh sách tạm
        boolean replaced = false;
        for (int i = 0; i < tempList.size(); i++) {
            if (tempList.get(i).getMaNH().equals(bank.getMaNH())) {
                tempList.set(i, bank);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            tempList.add(bank);
        }

        session.setAttribute("temporaryBanks", tempList);
        redirectAttributes.addFlashAttribute("message", "Đã cập nhật tạm ngân hàng " + bank.getMaNH());
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/banks";
    }

    @PostMapping("/banks/save-all")
    public String saveAllBanks(HttpSession session, RedirectAttributes redirectAttributes) {
        @SuppressWarnings("unchecked")
        List<NganHangTemp> tempList = (List<NganHangTemp>) session.getAttribute("temporaryBanks");

        if (tempList != null && !tempList.isEmpty()) {
            try {
                for (NganHangTemp temp : tempList) {
                    if (temp.isDaXoa()) {
                        nganHangService.xoaNganHang(temp.getMaNH());
                    } else {
                        Optional<NganHang> existing = nganHangService.findById(temp.getMaNH());
                        if (existing.isPresent()) {
                            nganHangService.capNhatNganHang(temp.getMaNH(), temp.toNganHangEntity());
                        } else {
                            nganHangService.themNganHang(temp.toNganHangEntity());
                        }
                    }
                }
                session.removeAttribute("temporaryBanks");
                redirectAttributes.addFlashAttribute("message", "Đã ghi vào cơ sở dữ liệu!");
                redirectAttributes.addFlashAttribute("messageType", "success");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("message", "Lỗi khi lưu ngân hàng: " + e.getMessage());
                redirectAttributes.addFlashAttribute("messageType", "error");
            }
        } else {
            redirectAttributes.addFlashAttribute("message", "Không có ngân hàng tạm để ghi.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/banks";
    }

    @PostMapping("/banks/delete")
    public String markBankDeleted(@RequestParam String maNH, HttpSession session, RedirectAttributes redirectAttributes) {
    	
    	if (taiKhoanNganHangRepository.existsByMaNH(maNH)) {
            redirectAttributes.addFlashAttribute("message", "Không thể xóa ngân hàng '" + maNH + "' vì đang được sử dụng trong tài khoản nhà đầu tư.");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/banks";
        }
    	
        @SuppressWarnings("unchecked")
        List<NganHangTemp> tempList = (List<NganHangTemp>) session.getAttribute("temporaryBanks");
        if (tempList == null) tempList = new ArrayList<>();

        Optional<NganHangTemp> temp = tempList.stream().filter(t -> t.getMaNH().equals(maNH)).findFirst();
        if (temp.isPresent()) {
            temp.get().setDaXoa(true);
        } else {
            Optional<NganHang> nh = nganHangService.findById(maNH);
            List<NganHangTemp> finalTempList = tempList;
            nh.ifPresent(n -> {
                NganHangTemp t = new NganHangTemp(n);
                t.setDaXoa(true);
                finalTempList.add(t);
            });
        }

        session.setAttribute("temporaryBanks", tempList);
        redirectAttributes.addFlashAttribute("message", "Đã đánh dấu xóa ngân hàng " + maNH);
        redirectAttributes.addFlashAttribute("messageType", "info");

        return "redirect:/banks";
    }

    @PostMapping("/banks/remove-temp")
    public String removeTempBank(@RequestParam String maNH,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        @SuppressWarnings("unchecked")
        List<NganHangTemp> tempList = (List<NganHangTemp>) session.getAttribute("temporaryBanks");

        if (tempList != null) {
            boolean removed = tempList.removeIf(emp -> emp.getMaNH().equals(maNH));
            session.setAttribute("temporaryBanks", tempList);

            if (removed) {
                redirectAttributes.addFlashAttribute("message", "Đã xóa khỏi danh sách tạm ngân hàng có mã " + maNH);
                redirectAttributes.addFlashAttribute("messageType", "success");
            } else {
                redirectAttributes.addFlashAttribute("message", "Không tìm thấy ngân hàng tạm có mã " + maNH);
                redirectAttributes.addFlashAttribute("messageType", "warning");
            }
        } else {
            redirectAttributes.addFlashAttribute("message", "Danh sách tạm không tồn tại.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect /banks";
    }

    @GetMapping("/banks/reload")
    public String reloadBanks(HttpSession session) {
        session.removeAttribute("temporaryBanks");
        return "redirect:/banks";
    }

    @PostMapping("/banks/search")
    public String searchBanks(@RequestParam String query, Model model, HttpSession session) {
        List<NganHang> dbResults = nganHangService.searchBanks(query);

        @SuppressWarnings("unchecked")
        List<NganHangTemp> tempList = (List<NganHangTemp>) session.getAttribute("temporaryBanks");
        if (tempList == null) {
            tempList = new ArrayList<>();
        }

        // Chuyển dbResults thành NganHangTemp
        List<NganHangTemp> searchResults = dbResults.stream()
                .map(NganHangTemp::new)
                .collect(Collectors.toList());

        // Thêm các ngân hàng tạm không bị xóa và khớp từ khóa
        for (NganHangTemp temp : tempList) {
            if (!temp.isDaXoa()) {
                boolean match = temp.getMaNH().toLowerCase().contains(query.toLowerCase())
                        || temp.getTenNH().toLowerCase().contains(query.toLowerCase())
                        || temp.getDiaChi().toLowerCase().contains(query.toLowerCase());
                if (match) {
                    searchResults.add(temp);
                }
            }
        }

        // Gói kết quả vào Page<NganHangTemp>
        Page<NganHangTemp> bankPage = new PageImpl<>(searchResults, PageRequest.of(0, Integer.MAX_VALUE), searchResults.size());

        // Thêm temporaryMaNHList
        List<String> temporaryMaNHList = tempList.stream()
                .filter(t -> !t.isDaXoa())
                .map(NganHangTemp::getMaNH)
                .toList();

        model.addAttribute("banks", bankPage);
        model.addAttribute("temporaryBanks", tempList);
        model.addAttribute("temporaryMaNHList", temporaryMaNHList);
        model.addAttribute("canUndo", !nganHangService.isUndoStackEmpty());

        if (searchResults.isEmpty()) {
            model.addAttribute("message", "Không tìm thấy ngân hàng nào.");
            model.addAttribute("messageType", "danger");
        }

        // Logging để debug
        System.out.println("searchBanks - searchResults size: " + searchResults.size());
        System.out.println("searchBanks - temporaryMaNHList: " + temporaryMaNHList);
        System.out.println("searchBanks - temporaryBanks size: " + tempList.size());
        System.out.println("searchBanks - banks class: " + bankPage.getClass().getName());
        System.out.println("searchBanks - banks.content size: " + bankPage.getContent().size());

        return "nhanvien/bank_list";
    }

    @PostMapping("/banks/undo")
    public String undoLastAction(RedirectAttributes redirectAttributes) {
        boolean success = nganHangService.undoThaoTacCuoi();
        if (success) {
            redirectAttributes.addFlashAttribute("message", "Hoàn tác thành công");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } else {
            redirectAttributes.addFlashAttribute("message", "Không có thao tác để hoàn tác");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/banks";
    }

    @PostMapping("/banks/undo-delete")
    public String undoDelete(@RequestParam("maNH") String maNH, HttpSession session,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "5") int size,
                             RedirectAttributes redirectAttributes) {

        @SuppressWarnings("unchecked")
        List<NganHangTemp> tempList = (List<NganHangTemp>) session.getAttribute("temporaryBanks");
        if (tempList == null) {
            tempList = new ArrayList<>();
        }

        Iterator<NganHangTemp> iterator = tempList.iterator();
        while (iterator.hasNext()) {
            NganHangTemp temp = iterator.next();
            if (temp.getMaNH().equals(maNH) && temp.isDaXoa()) {
                iterator.remove();
                redirectAttributes.addFlashAttribute("message", "Đã hoàn tác xóa cho ngân hàng " + maNH);
                redirectAttributes.addFlashAttribute("messageType", "success");
                break;
            }
        }

        session.setAttribute("temporaryBanks", tempList);
        return "redirect:/banks?page=" + page + "&size=" + size;
    }

    @PostMapping("/banks/clear-undo")
    public String clearUndoStackAndExit() {
        nganHangService.clearUndoStack();
        return "redirect:/nhanvien/layout";
    }
}