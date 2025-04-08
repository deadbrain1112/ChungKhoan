//import chungkhoan.entity.NhaDauTu;
//import chungkhoan.service.NDTService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//import java.util.List;
//
//@Controller
//@RequestMapping("/register")
//public class RegisterController {
//
//    @Autowired
//    private NDTService ndtService;
//
//    @GetMapping("")
//    public String showRegister(Model model) {
//        model.addAttribute("user", new NhaDauTu()); // để th:object binding dữ liệu
//        model.addAttribute("ndt", ndtService.findAll()); // danh sách NDT
//        return "nhanvien/register";
//    }
//}
