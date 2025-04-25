package chungkhoan.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
        Page<CoPhieu> stockPage = coPhieuService.getPaginated(page, size);
        model.addAttribute("stocks", stockPage);
        if (stockPage.isEmpty()) {
            model.addAttribute("noDataMessage", "Không có dữ liệu cổ phiếu.");
        }
        @SuppressWarnings("unchecked")
        List<CoPhieu> tempList = (List<CoPhieu>) session.getAttribute("temporaryStocks");
        if (tempList == null) tempList = new ArrayList<>();
        model.addAttribute("temporaryStocks", tempList);
        model.addAttribute("canUndo", !coPhieuService.isUndoStackEmpty());
        return "nhanvien/stocks";
    }

    @PostMapping("/stocks/add-temp")
    public String addTempStock(@ModelAttribute CoPhieu stock,
                               HttpSession session,
                               RedirectAttributes ra) {
        @SuppressWarnings("unchecked")
        List<CoPhieu> tempList = (List<CoPhieu>) session.getAttribute("temporaryStocks");
        if (tempList == null) tempList = new ArrayList<>();
        tempList.add(stock);
        session.setAttribute("temporaryStocks", tempList);
        ra.addFlashAttribute("message", "Đã thêm tạm cổ phiếu " + stock.getMaCP());
        ra.addFlashAttribute("messageType", "success");
        return "redirect:/stocks";
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