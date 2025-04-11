package chungkhoan.service;

import chungkhoan.entity.NhanVien;
import chungkhoan.repository.NhanVienRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NhanVienService {

    @Autowired
    private NhanVienRepository nhanVienRepository;

    public NhanVien getNhanVienByUsername(String username) {
        return nhanVienRepository.findByMaNV(username); // username là mã NV
    }
}
