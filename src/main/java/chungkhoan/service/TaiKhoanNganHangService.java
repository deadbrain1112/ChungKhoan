package chungkhoan.service;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.repository.LenhDatRepository;
import chungkhoan.repository.TaiKhoanNganHangRepository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaiKhoanNganHangService {

    @Autowired
    private TaiKhoanNganHangRepository taiKhoanNganHangRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LenhDatRepository lenhDatRepository;
    public List<TaiKhoanNganHang> findByInvestorMaNDT(String maNDT) {
        return taiKhoanNganHangRepository.findByNhaDauTuMaNDT(maNDT);
    }

    public List<TaiKhoanNganHang> getAllByNDT(NhaDauTu nhaDauTu) {
        return taiKhoanNganHangRepository.findByNhaDauTu(nhaDauTu);
    }

//    public void save(TaiKhoanNganHang taiKhoanNganHang) {
//        taiKhoanNganHangRepository.save(taiKhoanNganHang);
//    }

    public Map<String, List<TaiKhoanNganHang>> getBankAccountsForInvestors(List<NhaDauTu> investors) {
        Map<String, List<TaiKhoanNganHang>> bankAccountMap = new HashMap<>();
        for (NhaDauTu ndt : investors) {
            List<TaiKhoanNganHang> accounts = taiKhoanNganHangRepository.findByNhaDauTu(ndt);
            bankAccountMap.put(ndt.getMaNDT(), accounts);
        }
        return bankAccountMap;
    }

    @Transactional
    public void deleteByInvestorMaNDT(String maNDT) {
        taiKhoanNganHangRepository.deleteByNhaDauTuMaNDT(maNDT);
    }

    @Transactional
    public void capNhatHoacThemTaiKhoanNganHang(TaiKhoanNganHang tknh) {
        try {
            if (tknh.getNganHang() == null || tknh.getNganHang().getMaNH() == null) {
                throw new IllegalArgumentException("NganHang hoặc MaNH không được null");
            }
            if (tknh.getNhaDauTu() == null || tknh.getNhaDauTu().getMaNDT() == null) {
                throw new IllegalArgumentException("NhaDauTu hoặc MaNDT không được null");
            }

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM taikhoan_nganhang WHERE MaTK = ?",
                    Integer.class,
                    tknh.getMaTK()
            );

            if (count != null && count > 0) {
                jdbcTemplate.execute((Connection conn) -> {
                    CallableStatement cs = conn.prepareCall("{call sp_CapNhatTaiKhoanNganHang(?, ?, ?, ?)}");
                    cs.setString(1, tknh.getMaTK());
                    cs.setString(2, tknh.getNhaDauTu().getMaNDT());
                    cs.setString(3, tknh.getNganHang().getMaNH());
                    cs.setBigDecimal(4, tknh.getSoTien());
                    cs.execute();
                    return null;
                });
            } else {
                themTaiKhoanNganHang(tknh);
            }
        } catch (Exception e) {
            System.out.println("Exception during capNhatHoacThemTaiKhoanNganHang: " + e.getMessage());
            throw new RuntimeException("Lỗi khi cập nhật hoặc thêm tài khoản ngân hàng: " + e.getMessage());
        }
    }


    @Transactional
    public void themTaiKhoanNganHang(TaiKhoanNganHang tknh) {
        try {
            if (tknh.getNganHang() == null || tknh.getNganHang().getMaNH() == null) {
                throw new IllegalArgumentException("NganHang hoặc MaNH không được null");
            }
            if (tknh.getNhaDauTu() == null || tknh.getNhaDauTu().getMaNDT() == null) {
                throw new IllegalArgumentException("NhaDauTu hoặc MaNDT không được null");
            }

            System.out.println("Input TaiKhoanNganHang: MaTK=" + tknh.getMaTK() + ", MaNDT=" + tknh.getNhaDauTu().getMaNDT() + ", MaNH=" + tknh.getNganHang().getMaNH());

            jdbcTemplate.execute(
                    (Connection conn) -> {
                        CallableStatement cs = conn.prepareCall("{call sp_ThemTaiKhoanNganHangMoi(?, ?, ?, ?)}");
                        cs.setString(1, tknh.getMaTK());
                        cs.setString(2, tknh.getNhaDauTu().getMaNDT());
                        cs.setString(3, tknh.getNganHang().getMaNH());
                        cs.setBigDecimal(4, tknh.getSoTien());
                        boolean executed = cs.execute();
                        return null;
                    }
            );
        } catch (Exception e) {
            System.out.println("Exception during themTaiKhoanNganHang: " + e.getMessage());
            throw new RuntimeException("Lỗi khi thêm tài khoản ngân hàng: " + e.getMessage());
        }
    }

    public List<TaiKhoanNganHang> findTaiKhoanNganHangInLenhDat() {
        return taiKhoanNganHangRepository.findTaiKhoanNganHangInLenhDat();
    }
    
    public boolean truTien(String maTK, BigDecimal soTien) {
        TaiKhoanNganHang tk = taiKhoanNganHangRepository.findById(maTK).orElse(null);
        if (tk == null || tk.getSoTien().compareTo(soTien) < 0) return false;
        tk.setSoTien(tk.getSoTien().subtract(soTien));
        taiKhoanNganHangRepository.save(tk);
        return true;
    }

    public void congTien(String maTK, BigDecimal soTien) {
        TaiKhoanNganHang tk = taiKhoanNganHangRepository.findById(maTK).orElse(null);
        if (tk != null) {
            tk.setSoTien(tk.getSoTien().add(soTien));
            taiKhoanNganHangRepository.save(tk);
        }
    }

    public List<TaiKhoanNganHang> findTaiKhoanNganInLenhDat() {
        List<String> maTKsInLenhDat = lenhDatRepository.findDistinctMaTK();
        return taiKhoanNganHangRepository.findByMaTKIn(maTKsInLenhDat);
    }

    public void deleteTaiKhoanNganHang(String maTK) {
        taiKhoanNganHangRepository.deleteById(maTK);
    }

    public boolean existsByMaTK(String maTK) {
        if (maTK == null || maTK.isEmpty()) {
            return false;
        }
        boolean exists = taiKhoanNganHangRepository.existsByMaTK(maTK);
        return exists;
    }

}