package chungkhoan.service;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.repository.TaiKhoanNganHangRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaiKhoanNganHangService {

    @Autowired
    private TaiKhoanNganHangRepository taiKhoanNganHangRepository;

    // Lấy danh sách tài khoản ngân hàng theo maNDT
    public List<TaiKhoanNganHang> findByInvestorMaNDT(String maNDT) {
        return taiKhoanNganHangRepository.findByNhaDauTuMaNDT(maNDT);
    }

    // Lấy danh sách tài khoản ngân hàng theo NhaDauTu
    public List<TaiKhoanNganHang> getAllByNDT(NhaDauTu nhaDauTu) {
        return taiKhoanNganHangRepository.findByNhaDauTu(nhaDauTu);
    }

    // Lưu một tài khoản ngân hàng
    public void save(TaiKhoanNganHang taiKhoanNganHang) {
        taiKhoanNganHangRepository.save(taiKhoanNganHang);
    }

    public Map<String, List<TaiKhoanNganHang>> getBankAccountsForInvestors(List<NhaDauTu> investors) {
        Map<String, List<TaiKhoanNganHang>> bankAccountMap = new HashMap<>();
        for (NhaDauTu ndt : investors) {
            List<TaiKhoanNganHang> accounts = taiKhoanNganHangRepository.findByNhaDauTu(ndt);
            bankAccountMap.put(ndt.getMaNDT(), accounts);
        }
        return bankAccountMap;
    }

    // Xóa tài khoản ngân hàng theo maNDT
    @Transactional
    public void deleteByInvestorMaNDT(String maNDT) {
        taiKhoanNganHangRepository.deleteByNhaDauTuMaNDT(maNDT);
    }
}