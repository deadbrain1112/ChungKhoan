package chungkhoan.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import chungkhoan.entity.LenhDat;
import chungkhoan.entity.LenhKhop;
import chungkhoan.repository.LenhDatRepository;
import chungkhoan.repository.LenhKhopRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KhopLenhService {
    private final LenhDatRepository lenhDatRepo;
    private final LenhKhopRepository lenhKhopRepo;
    private final TaiKhoanNganHangService taiKhoanNganHangService;
    private final SoHuuService soHuuService;
    private final CoPhieuService coPhieuService;

    @Transactional
    public void khopLenh(String maCP) {
        List<LenhDat> muaList = lenhDatRepo.findByCoPhieu_MaCPAndLoaiGDAndTrangThaiOrderByGiaDescNgayGDAsc(maCP, "M", "Chờ");
        List<LenhDat> banList = lenhDatRepo.findByCoPhieu_MaCPAndLoaiGDAndTrangThaiOrderByGiaAscNgayGDAsc(maCP, "B", "Chờ");

        for (LenhDat mua : muaList) {
            for (LenhDat ban : banList) {
                if (mua.getSoLuong() <= 0 || ban.getSoLuong() <= 0) continue;
                if (mua.getGia() >= ban.getGia()) {
                    int slKhop = Math.min(mua.getSoLuong(), ban.getSoLuong());
                    double giaKhop = ban.getGia();
                    BigDecimal tien = BigDecimal.valueOf(slKhop * giaKhop);

                    if (!taiKhoanNganHangService.truTien(mua.getTaiKhoanNganHang().getMaTK(), tien)) {
                        continue; // Bỏ qua nếu không đủ tiền
                    }
                    if (!soHuuService.giamSoHuu(ban.getTaiKhoanNganHang().getNhaDauTu(), maCP, slKhop)) {
                        taiKhoanNganHangService.congTien(mua.getTaiKhoanNganHang().getMaTK(), tien);
                        continue; // Bỏ qua nếu không đủ cổ phiếu
                    }

                    taiKhoanNganHangService.congTien(ban.getTaiKhoanNganHang().getMaTK(), tien);
                    soHuuService.tangSoHuu(mua.getTaiKhoanNganHang().getNhaDauTu(), maCP, slKhop);

                    // Cập nhật giá cổ phiếu
                    coPhieuService.capNhatGiaMoiNhat(maCP, giaKhop);

                    // Ghi khớp lệnh
                    LenhKhop lenhKhop = LenhKhop.builder()
                            .lenhDat(mua)
                            .ngayGioKhop(LocalDateTime.now())
                            .soLuongKhop(slKhop)
                            .giaKhop(giaKhop)
                            .kieuKhop((mua.getSoLuong() == slKhop && ban.getSoLuong() == slKhop) ? "Khớp hết" : "Khớp 1 phần")
                            .build();
                    lenhKhopRepo.save(lenhKhop);

                    // Cập nhật trạng thái lệnh
                    mua.setSoLuong(mua.getSoLuong() - slKhop);
                    ban.setSoLuong(ban.getSoLuong() - slKhop);
                    mua.setTrangThai(mua.getSoLuong() == 0 ? "Khớp" : "Một phần");
                    ban.setTrangThai(ban.getSoLuong() == 0 ? "Khớp" : "Một phần");

                    lenhDatRepo.save(mua);
                    lenhDatRepo.save(ban);

                    if (mua.getSoLuong() == 0) break;
                }
            }
        }
    }

    private boolean isTrongGioGiaoDich() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return false;
        boolean sang = !time.isBefore(LocalTime.of(9, 0)) && time.isBefore(LocalTime.of(12, 00));
        boolean chieu = !time.isBefore(LocalTime.of(13, 0)) && time.isBefore(LocalTime.of(23, 0));
        return sang || chieu;
    }

    @Scheduled(fixedDelay = 1000)
    public void khopLenhTuDong() {
        if (!isTrongGioGiaoDich()) {
            System.out.println("[KHOP LENH] Ngoai gio giao dich, khong thuc hien.");
            return;
        }
        List<String> dsMaCP = lenhDatRepo.findAllMaCPDangChoKhop();
        for (String maCP : dsMaCP) {
            khopLenh(maCP);
        }
    }
}
