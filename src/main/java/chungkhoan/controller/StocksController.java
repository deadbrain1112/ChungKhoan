package chungkhoan.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import chungkhoan.dto.CoPhieuTemp;
import chungkhoan.dto.NhaDauTuTemp;
import chungkhoan.entity.NhaDauTu;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import chungkhoan.entity.CoPhieu;
import chungkhoan.repository.LenhDatRepository;
import chungkhoan.service.CoPhieuService;
@Controller
public class StocksController {

    @Autowired
    private CoPhieuService coPhieuService;
    
    @Autowired
    private LenhDatRepository lenhDatRepository;

    @GetMapping("/stocks")
    public String listStocks(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "5") int size,
                             Model model,
                             HttpSession session) {
    	
    	if (session.getAttribute("nhanVien") == null) {
	        return "redirect:/login";
	    }

        List<CoPhieu> fromDb = coPhieuService.getPaginated(0, Integer.MAX_VALUE).getContent();

        @SuppressWarnings("unchecked")
        List<CoPhieuTemp> tempList = (List<CoPhieuTemp>) session.getAttribute("temporaryStocks");
        if (tempList == null) tempList = new ArrayList<>();

        List<CoPhieuTemp> finalTempList = tempList;
        List<CoPhieu> filteredDb = fromDb.stream()
                .filter(cp -> finalTempList.stream().noneMatch(t -> t.getMaCP().equals(cp.getMaCP())))
                .toList();

        List<CoPhieuTemp> allStocks = new ArrayList<>();
        for (CoPhieu cp : filteredDb) {
            allStocks.add(new CoPhieuTemp(cp));
        }

        allStocks.addAll(tempList);

        int totalItems = allStocks.size();
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

        List<CoPhieuTemp> pageContent = (start < end) ? allStocks.subList(start, end) : new ArrayList<>();

        Page<CoPhieuTemp> allStocksPage = new PageImpl<>(pageContent, PageRequest.of(page, size), totalItems);

        model.addAttribute("stocks", allStocksPage);
        model.addAttribute("temporaryStocks", tempList);
        model.addAttribute("canUndo", !coPhieuService.isUndoStackEmpty());

        List<String> temporaryMaCPList = tempList.stream()
                .filter(t -> !t.isDaXoa())
                .map(CoPhieuTemp::getMaCP)
                .toList();
        model.addAttribute("temporaryMaCPList", temporaryMaCPList);

        if (allStocksPage.isEmpty()) {
            model.addAttribute("message", "Không có dữ liệu cổ phiếu.");
            model.addAttribute("messageType", "danger");
        }

        return "nhanvien/stocks";
    }

    @PostMapping("/stocks/add-temp")
    public String addTempStock(@ModelAttribute CoPhieuTemp stock,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "5") int size,
                               HttpSession session,
                               RedirectAttributes ra) {
        @SuppressWarnings("unchecked")
        List<CoPhieuTemp> tempList = (List<CoPhieuTemp>) session.getAttribute("temporaryStocks");
        if (tempList == null) tempList = new ArrayList<>();

        boolean maCPTrung = coPhieuService.existsById(stock.getMaCP()) ||
                tempList.stream()
                        .filter(t -> !t.isDaXoa())
                        .anyMatch(t -> t.getMaCP().equals(stock.getMaCP()));

        if (maCPTrung) {
            ra.addFlashAttribute("message", "Mã CP " + stock.getMaCP() + " đã tồn tại!");
            ra.addFlashAttribute("messageType", "error");
            return "redirect:/stocks?page=" + page + "&size=" + size;
        }

        tempList.add(stock);
        session.setAttribute("temporaryStocks", tempList);
        ra.addFlashAttribute("message", "Đã thêm tạm cổ phiếu " + stock.getMaCP());
        ra.addFlashAttribute("messageType", "success");

        return "redirect:/stocks?page=" + page + "&size=" + size;
    }

    @PostMapping("/stocks/edit-temp")
    public String editTempStock(@ModelAttribute CoPhieuTemp stock,
                                HttpSession session,
                                RedirectAttributes ra) {
        @SuppressWarnings("unchecked")
        List<CoPhieuTemp> tempList = (List<CoPhieuTemp>) session.getAttribute("temporaryStocks");
        if (tempList == null) tempList = new ArrayList<>();

        boolean replaced = false;
        for (int i = 0; i < tempList.size(); i++) {
            if (tempList.get(i).getMaCP().equals(stock.getMaCP())) {
                tempList.set(i, stock);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            tempList.add(stock);
        }
        session.setAttribute("temporaryStocks", tempList);
        ra.addFlashAttribute("message", "Đã cập nhật tạm cổ phiếu " + stock.getMaCP());
        ra.addFlashAttribute("messageType", "success");
        return "redirect:/stocks";
    }

    @PostMapping("/stocks/delete")
    public String markStockDeleted(@RequestParam String maCP, HttpSession session, RedirectAttributes ra) {
    	
        if (lenhDatRepository.existsByCoPhieu_MaCP(maCP)) {
            ra.addFlashAttribute("message", "Không thể xóa cổ phiếu '" + maCP + "' vì đã có lệnh giao dịch.");
            ra.addFlashAttribute("messageType", "error");
            return "redirect:/stocks";
        }
        
        @SuppressWarnings("unchecked")
        List<CoPhieuTemp> tempList = (List<CoPhieuTemp>) session.getAttribute("temporaryStocks");
        if (tempList == null) tempList = new ArrayList<>();

        Optional<CoPhieuTemp> temp = tempList.stream().filter(t -> t.getMaCP().equals(maCP)).findFirst();
        if (temp.isPresent()) {
            temp.get().setDaXoa(true);
        } else {
            Optional<CoPhieu> cp = coPhieuService.findById(maCP);
            List<CoPhieuTemp> finalTempList = tempList;
            cp.ifPresent(c -> {
                CoPhieuTemp t = new CoPhieuTemp(c);
                t.setDaXoa(true);
                finalTempList.add(t);
            });
        }

        session.setAttribute("temporaryStocks", tempList);
        ra.addFlashAttribute("message", "Đã đánh dấu xóa cổ phiếu " + maCP);
        ra.addFlashAttribute("messageType", "info");

        return "redirect:/stocks";
    }
    @PostMapping("/stocks/search")
    public String searchStocks(@RequestParam String query, Model model, HttpSession session) {
        List<CoPhieu> dbResults = coPhieuService.searchStocks(query);

        @SuppressWarnings("unchecked")
        List<CoPhieuTemp> tempList = (List<CoPhieuTemp>) session.getAttribute("temporaryStocks");
        if (tempList == null) {
            tempList = new ArrayList<>();
        }

        // Chuyển dbResults thành CoPhieuTemp
        List<CoPhieuTemp> searchResults = dbResults.stream()
                .map(CoPhieuTemp::new)
                .collect(Collectors.toList());

        // Thêm các cổ phiếu tạm không bị xóa và khớp từ khóa
        for (CoPhieuTemp temp : tempList) {
            if (!temp.isDaXoa()) {
                boolean match = temp.getMaCP().toLowerCase().contains(query.toLowerCase())
                        || temp.getTenCty().toLowerCase().contains(query.toLowerCase())
                        || temp.getDiaChi().toLowerCase().contains(query.toLowerCase());
                if (match) {
                    searchResults.add(temp);
                }
            }
        }

        // Gói kết quả vào Page<CoPhieuTemp>
        Page<CoPhieuTemp> stockPage = new PageImpl<>(searchResults, PageRequest.of(0, Integer.MAX_VALUE), searchResults.size());

        // Thêm temporaryMaCPList
        List<String> temporaryMaCPList = tempList.stream()
                .filter(t -> !t.isDaXoa())
                .map(CoPhieuTemp::getMaCP)
                .toList();

        model.addAttribute("stocks", stockPage);
        model.addAttribute("temporaryStocks", tempList);
        model.addAttribute("temporaryMaCPList", temporaryMaCPList);
        model.addAttribute("canUndo", !coPhieuService.isUndoStackEmpty());

        if (searchResults.isEmpty()) {
            model.addAttribute("message", "Không tìm thấy cổ phiếu nào.");
            model.addAttribute("messageType", "danger");
        }

        return "nhanvien/stocks";
    }

    @PostMapping("/stocks/save-all")
    public String saveAllStocks(HttpSession session, RedirectAttributes ra) {
        @SuppressWarnings("unchecked")
        List<CoPhieuTemp> tempList = (List<CoPhieuTemp>) session.getAttribute("temporaryStocks");

        if (tempList != null && !tempList.isEmpty()) {
            try {
                for (CoPhieuTemp temp : tempList) {
                    if (temp.isDaXoa()) {
                        coPhieuService.xoaCoPhieu(temp.getMaCP());
                    } else {
                        Optional<CoPhieu> existing = coPhieuService.findById(temp.getMaCP());
                        if (existing.isPresent()) {
                            coPhieuService.capNhatCoPhieu(temp.getMaCP(), temp.toCoPhieuEntity());
                        } else {
                            coPhieuService.themCoPhieuBangSP(temp.toCoPhieuEntity());
                        }
                    }
                }
                session.removeAttribute("temporaryStocks");
                ra.addFlashAttribute("message", "Đã ghi vào cơ sở dữ liệu!");
                ra.addFlashAttribute("messageType", "success");
            } catch (Exception e) {
                ra.addFlashAttribute("message", "Lỗi khi lưu cổ phiếu: " + e.getMessage());
                ra.addFlashAttribute("messageType", "error");
            }
        } else {
            ra.addFlashAttribute("message", "Không có cổ phiếu tạm để ghi.");
            ra.addFlashAttribute("messageType", "error");
        }

        return "redirect:/stocks";
    }

    @PostMapping("/stocks/remove-temp")
    public String removeTempStock(@RequestParam String maCP, HttpSession session, RedirectAttributes ra) {
        @SuppressWarnings("unchecked")
        List<CoPhieuTemp> tempList = (List<CoPhieuTemp>) session.getAttribute("temporaryStocks");

        if (tempList != null) {
            boolean removed = tempList.removeIf(cp -> cp.getMaCP().equals(maCP));
            session.setAttribute("temporaryStocks", tempList);

            if (removed) {
                ra.addFlashAttribute("message", "Đã xóa khỏi danh sách tạm cổ phiếu có mã " + maCP);
                ra.addFlashAttribute("messageType", "success");
            } else {
                ra.addFlashAttribute("message", "Không tìm thấy cổ phiếu tạm có mã " + maCP);
                ra.addFlashAttribute("messageType", "warning");
            }
        } else {
            ra.addFlashAttribute("message", "Danh sách tạm không tồn tại.");
            ra.addFlashAttribute("messageType", "error");
        }

        return "redirect:/stocks";
    }

    @PostMapping("/stocks/undo-delete")
    public String undoDelete(@RequestParam String maCP, HttpSession session,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "5") int size,
                             RedirectAttributes ra) {
        @SuppressWarnings("unchecked")
        List<CoPhieuTemp> tempList = (List<CoPhieuTemp>) session.getAttribute("temporaryStocks");
        if (tempList == null) tempList = new ArrayList<>();

        for (Iterator<CoPhieuTemp> it = tempList.iterator(); it.hasNext(); ) {
            CoPhieuTemp temp = it.next();
            if (temp.getMaCP().equals(maCP) && temp.isDaXoa()) {
                it.remove();
                ra.addFlashAttribute("message", "Đã hoàn tác xóa cổ phiếu " + maCP);
                ra.addFlashAttribute("messageType", "success");
                break;
            }
        }

        session.setAttribute("temporaryStocks", tempList);
        return "redirect:/stocks?page=" + page + "&size=" + size;
    }

    @GetMapping("/stocks/reload")
    public String reloadStocks(HttpSession session) {
        session.removeAttribute("temporaryStocks");
        return "redirect:/stocks";
    }

    @PostMapping("/stocks/undo")
    public String undo(RedirectAttributes ra) {
        boolean ok = coPhieuService.undoThaoTacCuoi();
        if (ok) {
            ra.addFlashAttribute("message", "Hoàn tác thành công");
            ra.addFlashAttribute("messageType", "success");
        } else {
            ra.addFlashAttribute("message", "Không có thao tác để hoàn tác");
            ra.addFlashAttribute("messageType", "error");
        }
        return "redirect:/stocks";
    }

    @PostMapping("/stocks/clear-undo")
    public String clearUndo() {
        coPhieuService.clearUndoStack();
        return "redirect:/nhanvien/layout";
    }
}
