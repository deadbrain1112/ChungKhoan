package chungkhoan.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.SoHuu;
import chungkhoan.repository.CoPhieuRepository;
import chungkhoan.repository.SoHuuRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SoHuuService {

    private final SoHuuRepository soHuuRepository;
    private final CoPhieuRepository coPhieuRepository;

    // Lấy danh sách sở hữu của một nhà đầu tư
    public List<SoHuu> getSoHuuByNDT(String maNDT) {
        return soHuuRepository.findByMaNDT(maNDT);
    }

    // Lấy danh sách mã cổ phiếu mà nhà đầu tư sở hữu
    public List<String> getMaCPByNDT(String maNDT) {
        return soHuuRepository.findMaCPByMaNDT(maNDT);
    }

    // Lấy số lượng cổ phiếu mà nhà đầu tư sở hữu cho một mã cổ phiếu
    public int getSoLuong(String maNDT, String maCP) {
        return soHuuRepository.findByMaNDTAndMaCP(maNDT, maCP)
                .map(SoHuu::getSoLuong)
                .orElse(0);
    }

    public boolean giamSoHuu(NhaDauTu ndt, String maCP, int soLuong) {
        return soHuuRepository.findByMaNDTAndMaCP(ndt.getMaNDT(), maCP).map(sh -> {
            if (sh.getSoLuong() < soLuong) return false;
            sh.setSoLuong(sh.getSoLuong() - soLuong);
            soHuuRepository.save(sh);
            return true;
        }).orElse(false);
    }

    public void tangSoHuu(NhaDauTu ndt, String maCP, int soLuong) {
        SoHuu soHuu = soHuuRepository.findByMaNDTAndMaCP(ndt.getMaNDT(), maCP)
                .orElseGet(() -> {
                    CoPhieu cp = coPhieuRepository.findById(maCP)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy cổ phiếu: " + maCP));
                    return new SoHuu(ndt.getMaNDT(), maCP, ndt, cp, 0);
                });

        soHuu.setSoLuong(soHuu.getSoLuong() + soLuong);
        soHuuRepository.save(soHuu);
    }
}
