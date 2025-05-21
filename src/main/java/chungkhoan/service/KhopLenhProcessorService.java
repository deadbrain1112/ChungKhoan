package chungkhoan.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import chungkhoan.entity.LenhDat;
import chungkhoan.entity.LenhKhop;
import chungkhoan.repository.LenhDatRepository;
import chungkhoan.repository.LenhKhopRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KhopLenhProcessorService {
    @Autowired
    private LenhDatRepository lenhDatRepo;

    @Autowired
    private LenhKhopRepository lenhKhopRepo;

    @Autowired
    private TaiKhoanNganHangService taiKhoanNganHangService;

    @Autowired
    private SoHuuService soHuuService;

    @Autowired
    private CoPhieuService coPhieuService;

    @Transactional
    public void khopLenh(String maCP) {
        System.out.println(">>> Bắt đầu khớp cho mã cổ phiếu: " + maCP);
        List<LenhDat> muaList = lenhDatRepo.findByCoPhieu_MaCPAndLoaiGDAndTrangThaiInOrderByGiaDescNgayGDAsc(
                maCP, "M", Arrays.asList("Chờ", "Một phần"));
        List<LenhDat> banList = lenhDatRepo.findByCoPhieu_MaCPAndLoaiGDAndTrangThaiInOrderByGiaAscNgayGDAsc(
                maCP, "B", Arrays.asList("Chờ", "Một phần"));

        for (LenhDat mua : muaList) {
            for (LenhDat ban : banList) {
                if (mua.getSoLuong() <= 0 || ban.getSoLuong() <= 0) continue;
                if (mua.getGia() >= ban.getGia()) {
                    int slKhop = Math.min(mua.getSoLuong(), ban.getSoLuong());
                    double giaKhop = ban.getGia();
                    BigDecimal tien = BigDecimal.valueOf(slKhop * giaKhop);

                    String maNguoiMua = mua.getTaiKhoanNganHang().getMaTK();
                    String maNguoiBan = ban.getTaiKhoanNganHang().getMaTK();
                    String maNDTBan = ban.getTaiKhoanNganHang().getNhaDauTu().getMaNDT();
                    String maNDTMua = mua.getTaiKhoanNganHang().getNhaDauTu().getMaNDT();

                    if (!taiKhoanNganHangService.truTien(maNguoiMua, tien)) {
                        continue;
                    }

                    if (!soHuuService.giamSoHuu(maNDTBan, maCP, slKhop)) {
                        taiKhoanNganHangService.congTien(maNguoiMua, tien);
                        continue;
                    }

                    taiKhoanNganHangService.congTien(maNguoiBan, tien);
                    soHuuService.tangSoHuu(maNDTMua, maCP, slKhop);
                    coPhieuService.capNhatGiaMoiNhat(maCP, giaKhop);

                    String kieuKhop = (mua.getSoLuong() == slKhop && ban.getSoLuong() == slKhop) ? "Khớp hết" : "Khớp 1 phần";

                    lenhKhopRepo.save(LenhKhop.builder()
                            .lenhDat(mua)
                            .ngayGioKhop(LocalDateTime.now())
                            .soLuongKhop(slKhop)
                            .giaKhop(giaKhop)
                            .kieuKhop(kieuKhop)
                            .build());

                    lenhKhopRepo.save(LenhKhop.builder()
                            .lenhDat(ban)
                            .ngayGioKhop(LocalDateTime.now())
                            .soLuongKhop(slKhop)
                            .giaKhop(giaKhop)
                            .kieuKhop(kieuKhop)
                            .build());

                    mua.setSoLuong(mua.getSoLuong() - slKhop);
                    ban.setSoLuong(ban.getSoLuong() - slKhop);

                    mua.setTrangThai(mua.getSoLuong() == 0 ? "Hết" : "Một phần");
                    ban.setTrangThai(ban.getSoLuong() == 0 ? "Hết" : "Một phần");

                    lenhDatRepo.save(mua);
                    lenhDatRepo.save(ban);

                    if (mua.getSoLuong() == 0) break;
                }
            }
        }
    }
}
