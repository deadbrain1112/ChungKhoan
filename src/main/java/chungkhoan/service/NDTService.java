package chungkhoan.service;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.repository.NDTRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;

@Service
public class NDTService {
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private NDTRepository ndtRepository;

    public boolean authenticateUser(String username, String password, HttpSession session) {
        boolean isValid = databaseService.testConnection(username, password);
        if (isValid) {
            session.setAttribute("username", username);
        }
        return isValid;
    }

    public void themNhaDauTuBangSP(NhaDauTu ndt) {
        ndtRepository.themNhaDauTu(
                ndt.getMaNDT(),
                ndt.getHoTen(),
                Date.valueOf(ndt.getNgaySinh()),
                ndt.getMkGiaoDich(),
                ndt.getDiaChi(),
                ndt.getPhone(),
                ndt.getCmnd(),
                ndt.getGioiTinh(),
                ndt.getEmail()
        );
    }
    
    public NhaDauTu getNhaDauTuByUsername(String username) {
        return ndtRepository.findByMaNDT(username);
    }
}
