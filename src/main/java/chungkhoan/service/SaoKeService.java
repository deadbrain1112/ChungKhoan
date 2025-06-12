package chungkhoan.service;

import chungkhoan.repository.SaoKeRepository;
import chungkhoan.repository.TaiKhoanNganHangRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SaoKeService {

    private final SaoKeRepository saoKeRepository;
    private final TaiKhoanNganHangRepository taiKhoanRepo;

    public SaoKeService(SaoKeRepository saoKeRepository, TaiKhoanNganHangRepository taiKhoanRepo) {
        this.saoKeRepository = saoKeRepository;
        this.taiKhoanRepo = taiKhoanRepo;
    }

    public List<Object[]> getSaoKeAllTaiKhoan(String maNDT, String maCP, LocalDateTime from, LocalDateTime to) {
        List<String> danhSachMaTK = taiKhoanRepo.findMaTKByMaNDT(maNDT);
        if (danhSachMaTK.isEmpty()) {
            throw new RuntimeException("Không tìm thấy tài khoản ngân hàng nào cho nhà đầu tư: " + maNDT);
        }

        List<Object[]> tongKetQua = new java.util.ArrayList<>();

        for (String maTK : danhSachMaTK) {
            List<Object[]> ketQua = saoKeRepository.callSaoKeGiaoDich(maNDT, maTK, maCP, from, to);
            tongKetQua.addAll(ketQua);
        }

        return tongKetQua;
    }
}