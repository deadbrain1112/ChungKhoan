package chungkhoan.service;

import chungkhoan.entity.LenhDat;
import chungkhoan.repository.LenhDatRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
