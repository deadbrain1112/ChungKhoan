package chungkhoan.controller;

import java.util.Optional;

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
                             @RequestParam(name = "action", required = false) String action,
                             @RequestParam(name = "maCP", required = false) String maCP,
                             Model model) {
        Page<CoPhieu> stockPage = coPhieuService.getPaginated(page, size);
        model.addAttribute("stocks", stockPage);

        if (stockPage.isEmpty()) {
            model.addAttribute("noDataMessage", "Không có dữ liệu cổ phiếu.");
        }

        // Đối với hành động "edit"
        if ("edit".equals(action) && maCP != null) {
            Optional<CoPhieu> optional = coPhieuService.findById(maCP);
            optional.ifPresentOrElse(
                stock -> model.addAttribute("stock", stock),
                () -> model.addAttribute("stock", new CoPhieu())
            );
            model.addAttribute("formMode", "edit");
        } else if ("add".equals(action)) {
            model.addAttribute("stock", new CoPhieu());
            model.addAttribute("formMode", "add");
        } else {
            model.addAttribute("stock", new CoPhieu());
        }

        model.addAttribute("canUndo", !coPhieuService.isUndoStackEmpty());
        return "nhanvien/stocks";
    }

    @PostMapping("/stocks/add")
    public String addStock(@ModelAttribute("stock") CoPhieu coPhieu) {
        try {
            coPhieuService.themCoPhieuBangSP(coPhieu);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/stocks";
    }

    @PostMapping("/stocks/edit")
    public String editStock(@RequestParam("maCP") String maCP,
                            @ModelAttribute("stock") CoPhieu coPhieu) {
        // Sửa cổ phiếu với mã cổ phiếu
        coPhieuService.capNhatCoPhieu(maCP, coPhieu);
        return "redirect:/stocks";
    }

    @PostMapping("/stocks/delete")
    public String deleteStock(@RequestParam("maCP") String maCP, RedirectAttributes redirectAttributes) {
        try {
            // Xóa cổ phiếu
            coPhieuService.xoaCoPhieu(maCP);
            redirectAttributes.addFlashAttribute("message", "Cổ phiếu đã được xóa thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Cổ phiếu này đã được đặt");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/stocks";
    }

    @PostMapping("/stocks/search")
    public String searchStock(@RequestParam("query") String query, Model model) {
        // Chức năng tìm kiếm (hiện tại chỉ điều hướng lại trang)
        return "redirect:/stocks";
    }

    @PostMapping("/stocks/undo")
    public String undoLastAction(RedirectAttributes redirectAttributes) {
        boolean success = coPhieuService.undoThaoTacCuoi();
        if (success) {
            redirectAttributes.addFlashAttribute("message", "Hoàn tác thành công");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } else {
            redirectAttributes.addFlashAttribute("message", "Không có thao tác để hoàn tác");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/stocks";
    }
    
    @PostMapping("/stocks/clear-undo")
	public String clearUndoStackAndExit() {
    	coPhieuService.clearUndoStack();
	    return "redirect:/nhanvien/layout"; // hoặc bất kỳ trang nào bạn muốn về khi thoát
	}
}
