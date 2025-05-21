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

        List<LenhDat> muaList = lenhDatRepo
                .findByCoPhieu_MaCPAndLoaiGDAndTrangThaiInOrderByGiaDescNgayGDAsc(maCP, "M", Arrays.asList("Chờ", "Một phần"));

        List<LenhDat> banList = lenhDatRepo
                .findByCoPhieu_MaCPAndLoaiGDAndTrangThaiInOrderByGiaAscNgayGDAsc(maCP, "B", Arrays.asList("Chờ", "Một phần"));

        int banIndex = 0;
        for (LenhDat mua : muaList) {
            while (mua.getSoLuong() > 0 && banIndex < banList.size()) {
                LenhDat ban = banList.get(banIndex);
                if (ban.getSoLuong() <= 0) {
                    banIndex++;
                    continue;
                }

                if (mua.getGia() < ban.getGia()) break;

                xuLyGiaoDich(maCP, mua, ban);

                if (ban.getSoLuong() <= 0) banIndex++;
            }
        }
    }

    private void xuLyGiaoDich(String maCP, LenhDat mua, LenhDat ban) {
        int slKhop = Math.min(mua.getSoLuong(), ban.getSoLuong());
        double giaKhop = ban.getGia();
        BigDecimal tien = BigDecimal.valueOf(slKhop * giaKhop);

        String maNguoiMua = mua.getTaiKhoanNganHang().getMaTK();
        String maNguoiBan = ban.getTaiKhoanNganHang().getMaTK();
        String maNDTBan = ban.getTaiKhoanNganHang().getNhaDauTu().getMaNDT();
        String maNDTMua = mua.getTaiKhoanNganHang().getNhaDauTu().getMaNDT();

        if (!taiKhoanNganHangService.truTien(maNguoiMua, tien)) return;

        if (!soHuuService.giamSoHuu(maNDTBan, maCP, slKhop)) {
            taiKhoanNganHangService.congTien(maNguoiMua, tien); // hoàn tiền nếu thất bại
            return;
        }

        // Thực hiện giao dịch thành công
        taiKhoanNganHangService.congTien(maNguoiBan, tien);
        soHuuService.tangSoHuu(maNDTMua, maCP, slKhop);
        coPhieuService.capNhatGiaMoiNhat(maCP, giaKhop);

        String kieuKhop = (mua.getSoLuong() == slKhop && ban.getSoLuong() == slKhop) ? "Khớp hết" : "Khớp 1 phần";

        lenhKhopRepo.save(LenhKhop.builder().lenhDat(mua).ngayGioKhop(LocalDateTime.now()).soLuongKhop(slKhop).giaKhop(giaKhop).kieuKhop(kieuKhop).build());
        lenhKhopRepo.save(LenhKhop.builder().lenhDat(ban).ngayGioKhop(LocalDateTime.now()).soLuongKhop(slKhop).giaKhop(giaKhop).kieuKhop(kieuKhop).build());

        capNhatTrangThai(mua, slKhop);
        capNhatTrangThai(ban, slKhop);
    }

    private void capNhatTrangThai(LenhDat lenh, int slKhop) {
        lenh.setSoLuong(lenh.getSoLuong() - slKhop);
        lenh.setTrangThai(lenh.getSoLuong() == 0 ? "Hết" : "Một phần");
        lenhDatRepo.save(lenh);
    }
}
