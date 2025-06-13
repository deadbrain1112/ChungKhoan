package chungkhoan.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import chungkhoan.service.CoPhieuService;

@Controller
public class StocksController {

    @Autowired
    private CoPhieuService coPhieuService;

    @GetMapping("/stocks")
    public String listStocks(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "5") int size,
                             Model model,
                             HttpSession session) {

        // Lấy danh sách cổ phiếu chính từ cơ sở dữ liệu
        Page<CoPhieu> stockPage = coPhieuService.getPaginated(0, Integer.MAX_VALUE); // Lấy tất cả để kết hợp


        @SuppressWarnings("unchecked")
        List<CoPhieu> tempList = (List<CoPhieu>) session.getAttribute("temporaryStocks");
        if (tempList == null) {
            tempList = new ArrayList<>();
        }

        // Kết hợp danh sách: danh sách chính trước, danh sách tạm sau
        List<CoPhieu> allStocks = new ArrayList<>();
        allStocks.addAll(stockPage.getContent()); // Thêm danh sách chính trước
        allStocks.addAll(tempList); // Thêm danh sách tạm sau

        // Tính toán phân trang cho danh sách kết hợp
        int totalItems = allStocks.size();
        int start = page * size;
        int end = Math.min(start + size, totalItems);

        // Đảm bảo start và end hợp lệ
        if (start >= totalItems && totalItems > 0) {
            // Nếu start vượt quá kích thước danh sách, chuyển về trang cuối cùng
            page = (totalItems - 1) / size;
            start = page * size;
            end = Math.min(start + size, totalItems);
        } else if (start >= totalItems) {
            start = 0;
            end = 0;
        }

        List<CoPhieu> allStocksPageContent = (start < end) ? allStocks.subList(start, end) : new ArrayList<>();

        // Tạo đối tượng Page cho danh sách kết hợp
        Page<CoPhieu> allStocksPage = new PageImpl<>(allStocksPageContent, PageRequest.of(page, size), totalItems);

        // Thêm vào model
        model.addAttribute("stocks", allStocksPage);
        model.addAttribute("temporaryStocks", tempList);
        if (allStocksPage.isEmpty()) {
            model.addAttribute("message", "Không có dữ liệu cổ phiếu.");
            model.addAttribute("messageType", "danger");
        }

        // Kiểm tra nếu có thể hoàn tác
        model.addAttribute("canUndo", !coPhieuService.isUndoStackEmpty());

        // Thêm logging để kiểm tra
        System.out.println("TempList size: " + tempList.size());
        System.out.println("AllStocks size: " + allStocks.size());
        System.out.println("Page content size: " + allStocksPageContent.size());

        return "nhanvien/stocks";
    }

    @PostMapping("/stocks/add-temp")
    public String addTempStock(@ModelAttribute CoPhieu stock,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "5") int size,
                               HttpSession session,
                               RedirectAttributes ra) {
        @SuppressWarnings("unchecked")
        List<CoPhieu> tempList = (List<CoPhieu>) session.getAttribute("temporaryStocks");
        if (tempList == null) {
            tempList = new ArrayList<>();
        }

        // Kiểm tra xem mã CP đã tồn tại trong danh sách tạm chưa
        boolean exists = tempList.stream().anyMatch(s -> s.getMaCP().equals(stock.getMaCP()));
        if (exists) {
            ra.addFlashAttribute("message", "Mã CP " + stock.getMaCP() + " đã tồn tại trong danh sách tạm!");
            ra.addFlashAttribute("messageType", "error");
        } else {
            tempList.add(stock);
            session.setAttribute("temporaryStocks", tempList);
            ra.addFlashAttribute("message", "Đã thêm tạm cổ phiếu " + stock.getMaCP());
            ra.addFlashAttribute("messageType", "success");

            // Thêm logging để kiểm tra
            System.out.println("Added stock to tempList: " + stock.getMaCP());
            System.out.println("TempList after adding: " + tempList);
        }

        // Chuyển hướng về trang hiện tại
        return "redirect:/stocks?page=" + page + "&size=" + size;
    }

    @PostMapping("/stocks/edit-temp")
    public String editTempStock(@ModelAttribute CoPhieu stock,
                                HttpSession session,
                                RedirectAttributes ra) {
        @SuppressWarnings("unchecked")
        List<CoPhieu> tempList = (List<CoPhieu>) session.getAttribute("temporaryStocks");
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

    @PostMapping("/stocks/save-all")
    public String saveAllStocks(HttpSession session, RedirectAttributes ra) {
        @SuppressWarnings("unchecked")
        List<CoPhieu> tempList = (List<CoPhieu>) session.getAttribute("temporaryStocks");
        if (tempList != null && !tempList.isEmpty()) {
            for (CoPhieu stock : tempList) {
                Optional<CoPhieu> existing = coPhieuService.findById(stock.getMaCP());
                if (existing.isPresent()) {
                    coPhieuService.capNhatCoPhieu(stock.getMaCP(), stock);
                } else {
                    coPhieuService.themCoPhieuBangSP(stock);
                }
            }
            session.removeAttribute("temporaryStocks");
            ra.addFlashAttribute("message", "Đã ghi vào cơ sở dữ liệu!");
            ra.addFlashAttribute("messageType", "success");
        } else {
            ra.addFlashAttribute("message", "Không có cổ phiếu tạm để ghi.");
            ra.addFlashAttribute("messageType", "error");
        }
        return "redirect:/stocks";
    }


    @PostMapping("/stocks/delete")
    public String deleteStock(@RequestParam String maCP, RedirectAttributes ra) {
        try {
            coPhieuService.xoaCoPhieu(maCP);
            ra.addFlashAttribute("message", "Đã xóa cổ phiếu " + maCP);
            ra.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("message", "Xóa thất bại");
            ra.addFlashAttribute("messageType", "error");
        }
        return "redirect:/stocks";
    }

    @PostMapping("/stocks/remove-temp")
    public String removeTempStock(@RequestParam String maCP, HttpSession session, RedirectAttributes ra) {
        @SuppressWarnings("unchecked")
        List<CoPhieu> tempList = (List<CoPhieu>) session.getAttribute("temporaryStocks");

        if (tempList != null) {
            tempList.removeIf(stock -> stock.getMaCP().equals(maCP));
            session.setAttribute("temporaryStocks", tempList);
            ra.addFlashAttribute("message", "Đã xóa cổ phiếu tạm có mã " + maCP);
            ra.addFlashAttribute("messageType", "success");
        } else {
            ra.addFlashAttribute("message", "Không tìm thấy cổ phiếu tạm nào để xóa.");
            ra.addFlashAttribute("messageType", "error");
        }

        return "redirect:/stocks";
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
