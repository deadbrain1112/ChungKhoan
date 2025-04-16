package chungkhoan.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import chungkhoan.entity.SoHuu;
import chungkhoan.repository.SoHuuRepository;

@Service
public class SoHuuService {

    @Autowired
    private SoHuuRepository soHuuRepository;

    // Lấy danh sách sở hữu của một nhà đầu tư
    public List<SoHuu> getSoHuuByNDT(String maNDT) {
        return soHuuRepository.findByMaNDT(maNDT);
    }

    // Lấy danh sách mã cổ phiếu mà nhà đầu tư sở hữu
    public List<String> getMaCPByNDT(String maNDT) {
        return soHuuRepository.findMaCPByMaNDT(maNDT);
    }
}