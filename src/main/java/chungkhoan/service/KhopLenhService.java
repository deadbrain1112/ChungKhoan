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
    	System.out.println(">>> Bắt đầu khớp cho mã cổ phiếu: " + maCP);
        List<LenhDat> muaList = lenhDatRepo.findByCoPhieu_MaCPAndLoaiGDAndTrangThaiOrderByGiaDescNgayGDAsc(maCP, "M", "Chờ");
        List<LenhDat> banList = lenhDatRepo.findByCoPhieu_MaCPAndLoaiGDAndTrangThaiOrderByGiaAscNgayGDAsc(maCP, "B", "Chờ");

        System.out.println(">>> Khớp lệnh cho mã: " + maCP);
        System.out.println(">>> Số lệnh MUA: " + muaList.size());
        System.out.println(">>> Số lệnh BÁN: " + banList.size());

        for (LenhDat mua : muaList) {
            for (LenhDat ban : banList) {
                System.out.println("Đang xét MUA#" + mua.getMaGD() + " vs BÁN#" + ban.getMaGD());

                if (mua.getSoLuong() <= 0 || ban.getSoLuong() <= 0) continue;
                if (mua.getGia() >= ban.getGia()) {
                    int slKhop = Math.min(mua.getSoLuong(), ban.getSoLuong());
                    double giaKhop = ban.getGia();
                    BigDecimal tien = BigDecimal.valueOf(slKhop * giaKhop);

                    if (!taiKhoanNganHangService.truTien(mua.getTaiKhoanNganHang().getMaTK(), tien)) {
                        System.out.println("Không đủ tiền để mua - bỏ qua.");
                        continue;
                    }

                    if (!soHuuService.giamSoHuu(ban.getTaiKhoanNganHang().getNhaDauTu(), maCP, slKhop)) {
                        System.out.println("Không đủ cổ phiếu để bán - hoàn tiền.");
                        taiKhoanNganHangService.congTien(mua.getTaiKhoanNganHang().getMaTK(), tien);
                        continue;
                    }

                    taiKhoanNganHangService.congTien(ban.getTaiKhoanNganHang().getMaTK(), tien);
                    soHuuService.tangSoHuu(mua.getTaiKhoanNganHang().getNhaDauTu(), maCP, slKhop);

                    coPhieuService.capNhatGiaMoiNhat(maCP, giaKhop);

                    // Ghi khớp cho MUA
                    lenhKhopRepo.save(LenhKhop.builder()
                            .lenhDat(mua)
                            .ngayGioKhop(LocalDateTime.now())
                            .soLuongKhop(slKhop)
                            .giaKhop(giaKhop)
                            .kieuKhop((mua.getSoLuong() == slKhop && ban.getSoLuong() == slKhop) ? "Khớp hết" : "Khớp 1 phần")
                            .build());

                    // Ghi khớp cho BÁN
                    lenhKhopRepo.save(LenhKhop.builder()
                            .lenhDat(ban)
                            .ngayGioKhop(LocalDateTime.now())
                            .soLuongKhop(slKhop)
                            .giaKhop(giaKhop)
                            .kieuKhop((mua.getSoLuong() == slKhop && ban.getSoLuong() == slKhop) ? "Khớp hết" : "Khớp 1 phần")
                            .build());

                    // Cập nhật trạng thái
                    mua.setSoLuong(mua.getSoLuong() - slKhop);
                    ban.setSoLuong(ban.getSoLuong() - slKhop);
                    mua.setTrangThai(mua.getSoLuong() == 0 ? "Khớp" : "Một phần");
                    ban.setTrangThai(ban.getSoLuong() == 0 ? "Khớp" : "Một phần");

                    lenhDatRepo.save(mua);
                    lenhDatRepo.save(ban);

                    System.out.println("✅ Khớp thành công " + slKhop + " cổ phiếu AAA @ " + giaKhop);

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
    	System.out.println("[SCHEDULED] Tự động kiểm tra khớp lệnh lúc " + LocalDateTime.now());
        if (!isTrongGioGiaoDich()) {
            System.out.println("[KHOP LENH] Ngoai gio giao dich, khong thuc hien.");
            return;
        }
        List<String> dsMaCP = lenhDatRepo.findAllMaCPDangChoKhop("Chờ");
        System.out.println(">>> Danh sách mã CP đang chờ khớp: " + dsMaCP);
        for (String maCP : dsMaCP) {
        	System.out.println(">>> Đang xử lý mã CP: " + maCP);
            khopLenh(maCP);
        }
    }
}
