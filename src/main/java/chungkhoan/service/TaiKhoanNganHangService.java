package chungkhoan.service;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.repository.TaiKhoanNganHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaiKhoanNganHangService {

    @Autowired
    private TaiKhoanNganHangRepository taiKhoanNganHangRepository;
    
    // Danh sách ngân hàng của nhà đầu tư nhất định
//    public TaiKhoanNganHang getTaiKhoanByNDT(NhaDauTu nhaDauTu) {
//        return taiKhoanNganHangRepository.findByNhaDauTu(nhaDauTu);
//    }
}
