package chungkhoan.service;

import chungkhoan.entity.LenhDat;
import chungkhoan.repository.LenhDatRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LenhDatService {
	
	@Autowired
    private LenhDatRepository lenhDatRepository;

    public List<LenhDat> timTheoMaNhaDauTu(String maNDT) {
        if (maNDT == null || maNDT.isBlank()) {
            throw new IllegalArgumentException("Không tìm thấy mã của nhà đầu tư.");
        }
        return lenhDatRepository.timLenhDatTheoMaNDT(maNDT);
    }
    
    public Optional<LenhDat> findById(Long maGD) {
        return lenhDatRepository.findById(maGD);
    }
    
    public void save(LenhDat lenhDat) {
    	lenhDatRepository.save(lenhDat);
    }
    
    public List<LenhDat> timTheoMaNhaDauTuVaTrangThai(String maNDT, String trangThai) {
        return lenhDatRepository.findByTaiKhoanNganHang_NhaDauTu_MaNDTAndTrangThai(maNDT, trangThai);
    }
    
    public List<LenhDat> timTheoMaNhaDauTuVaMaCPTrongKhoangNgayVaTrangThai(
            String maNDT, String maCP, LocalDateTime startDate, LocalDateTime endDate, String trangThai) {
        if (maNDT == null || maNDT.isBlank() || maCP == null || maCP.isBlank()) {
            throw new IllegalArgumentException("Mã nhà đầu tư và mã cổ phiếu bị rỗng.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
        return lenhDatRepository.findByMaNDTAndMaCPAndNgayGDAndTrangThai(maNDT, maCP, startDate, endDate, trangThai);
    }
}
