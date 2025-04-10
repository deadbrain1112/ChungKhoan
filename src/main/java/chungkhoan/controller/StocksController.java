package chungkhoan.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import chungkhoan.entity.CoPhieu;
import chungkhoan.service.CoPhieuService;

@Controller
public class StocksController {

    @Autowired
    private CoPhieuService coPhieuService;

    // Trang chính: hiển thị danh sách, có thể truyền form nếu đang "thêm" hoặc "ghi"
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
            // Trường hợp không có action, vẫn cần có stock để Thymeleaf không lỗi
            model.addAttribute("stock", new CoPhieu());
        }

        return "nhanvien/stocks";
    }

    // Thêm cổ phiếu
    @PostMapping("/stocks/add")
    public String addStock(@ModelAttribute("stock") CoPhieu coPhieu) {
        try {

            coPhieuService.themCoPhieuBangSP(coPhieu);
        } catch (Exception e) {
            // Có thể ghi log hoặc hiển thị lỗi nếu muốn
            e.printStackTrace();
        }
        return "redirect:/stocks";
    }


    // Ghi (chỉnh sửa) cổ phiếu
    @PostMapping("/stocks/edit/{maCP}")
    public String editStock(@PathVariable String maCP,
                            @ModelAttribute("stock") CoPhieu coPhieu) {
        coPhieu.setMaCP(maCP); // Đảm bảo giữ đúng mã cổ phiếu
        coPhieuService.save(coPhieu);
        return "redirect:/stocks";
    }

    // Xóa cổ phiếu
    @PostMapping("/stocks/delete/{maCP}")
    public String deleteStock(@PathVariable String maCP) {
        coPhieuService.deleteById(maCP);
        return "redirect:/stocks";
    }

    // Tìm kiếm (placeholder)
    @PostMapping("/stocks/search")
    public String searchStock(@RequestParam("query") String query, Model model) {
        // Bạn có thể thực hiện tìm kiếm thực sự ở đây
        // Tạm thời redirect về trang stocks
        return "redirect:/stocks";
    }
}
