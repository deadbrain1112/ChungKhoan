package chungkhoan.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import chungkhoan.entity.LichSuGia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    @Autowired private LenhDatRepository lenhDatRepo;
    @Autowired private LenhKhopRepository lenhKhopRepo;
    @Autowired private TaiKhoanNganHangService taiKhoanNganHangService;
    @Autowired private SoHuuService soHuuService;
    @Autowired private LichSuGiaService lichSuGiaService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    public enum Phase {
        ATO, LO, ATC
    }

    @Transactional
    public void khopLenh(String maCP, String phaseStr) {
        Phase phase = Phase.valueOf(phaseStr);
        List<LenhDat> all = lenhDatRepo.findKhoppableNative(maCP.trim(), Arrays.asList("Chờ", "Một phần"));

        List<LenhDat> loMua = all.stream()
                .filter(l -> "M".equalsIgnoreCase(l.getLoaiGD().trim()) && "LO".equalsIgnoreCase(l.getLoaiLenh().trim()))
                .sorted(Comparator.comparing(LenhDat::getGia).reversed().thenComparing(LenhDat::getNgayGD))
                .collect(Collectors.toList());

        List<LenhDat> loBan = all.stream()
                .filter(l -> "B".equalsIgnoreCase(l.getLoaiGD().trim()) && "LO".equalsIgnoreCase(l.getLoaiLenh().trim()))
                .sorted(Comparator.comparing(LenhDat::getGia).thenComparing(LenhDat::getNgayGD))
                .collect(Collectors.toList());


        List<LenhDat> atoMua = all.stream()
        	    .filter(l -> "M".equals(l.getLoaiGD()) && "ATO".equalsIgnoreCase(l.getLoaiLenh().trim()))
        	    .collect(Collectors.toList());

        	List<LenhDat> atoBan = all.stream()
        	    .filter(l -> "B".equals(l.getLoaiGD()) && "ATO".equalsIgnoreCase(l.getLoaiLenh().trim()))
        	    .collect(Collectors.toList());

        	List<LenhDat> atcMua = all.stream()
        		    .filter(l -> "M".equals(l.getLoaiGD()) && "ATC".equalsIgnoreCase(l.getLoaiLenh().trim()))
        		    .collect(Collectors.toList());

        		List<LenhDat> atcBan = all.stream()
        		    .filter(l -> "B".equals(l.getLoaiGD()) && "ATC".equalsIgnoreCase(l.getLoaiLenh().trim()))
        		    .collect(Collectors.toList());


        		if (phase == Phase.ATO) {
        		    if (!atoMua.isEmpty() && !atoBan.isEmpty()) {
        		        double giaThamChieu = lichSuGiaService.layGiaMoiNhat(maCP).getGiaTC();
        		        double giaKhop = tinhGiaKhopATX(atoMua, atoBan, loMua, loBan, giaThamChieu);

        		        atoMua.forEach(l -> l.setGia(giaKhop));
        		        atoBan.forEach(l -> l.setGia(giaKhop));

        		        atoMua.forEach(lenhDatRepo::save);
        		        atoBan.forEach(lenhDatRepo::save);

        		        loMua.addAll(atoMua);
        		        loBan.addAll(atoBan);

        		    } else {
        		        System.out.println("Không đủ 2 phía ATO, bỏ qua tính giá.");
        		    }
        		}

        		if (phase == Phase.ATC) {
        		    if (!atcMua.isEmpty() && !atcBan.isEmpty()) {
        		        double giaThamChieu = lichSuGiaService.layGiaMoiNhat(maCP).getGiaTC();
        		        double giaKhop = tinhGiaKhopATX(atcMua, atcBan, loMua, loBan, giaThamChieu);

        		        atcMua.forEach(l -> l.setGia(giaKhop));
        		        atcBan.forEach(l -> l.setGia(giaKhop));

        		        atcMua.forEach(lenhDatRepo::save);
        		        atcBan.forEach(lenhDatRepo::save);

        		        loMua.addAll(atcMua);
        		        loBan.addAll(atcBan);

        		    } else {
        		        System.out.println("Không đủ 2 phía ATC, bỏ qua tính giá.");
        		    }
        		}

        loMua.sort(Comparator.comparing(LenhDat::getGia).reversed().thenComparing(LenhDat::getNgayGD));
        loBan.sort(Comparator.comparing(LenhDat::getGia).thenComparing(LenhDat::getNgayGD));


        khopDanhSach(maCP, loMua, loBan);
    }

    private double tinhGiaKhopATX(List<LenhDat> muaAT, List<LenhDat> banAT, List<LenhDat> loMua, List<LenhDat> loBan, double giaTC) {
        Set<Double> tapGia = new HashSet<>();
        loMua.forEach(l -> tapGia.add(l.getGia()));
        loBan.forEach(l -> tapGia.add(l.getGia()));
        tapGia.add(giaTC);

        int maxKL = 0;
        double bestGia = giaTC;

        for (Double gia : tapGia) {
            int muaDuoc = muaAT.stream().mapToInt(LenhDat::getSoLuong).sum() +
                          loMua.stream().filter(l -> l.getGia() >= gia).mapToInt(LenhDat::getSoLuong).sum();
            int banDuoc = banAT.stream().mapToInt(LenhDat::getSoLuong).sum() +
                          loBan.stream().filter(l -> l.getGia() <= gia).mapToInt(LenhDat::getSoLuong).sum();

            int slKhop = Math.min(muaDuoc, banDuoc);
            if (slKhop > maxKL || (slKhop == maxKL && Math.abs(gia - giaTC) < Math.abs(bestGia - giaTC))) {
                maxKL = slKhop;
                bestGia = gia;
            }
        }

        return bestGia;
    }

    private void khopDanhSach(String maCP, List<LenhDat> muaList, List<LenhDat> banList) {
        int banIndex = 0;
        for (LenhDat mua : muaList) {
            while (mua.getSoLuong() > 0 && banIndex < banList.size()) {
                LenhDat ban = banList.get(banIndex);
                
                int oldMua = mua.getSoLuong();
                int oldBan = ban.getSoLuong();

                if (ban.getSoLuong() <= 0) {
                    banIndex++;
                    continue;
                }

                if (mua.getGia() < ban.getGia()) break;

                xuLyGiaoDich(maCP, mua, ban);
                
                if (mua.getSoLuong() == oldMua && ban.getSoLuong() == oldBan) {
                    System.out.println("Giao dịch không thay đổi số lượng, tránh lặp vô hạn: " + mua.getMaGD() + " - " + ban.getMaGD());
                    break;
                }

                if (ban.getSoLuong() <= 0) banIndex++;
            }
        }
    }

    private void xuLyGiaoDich(String maCP, LenhDat mua, LenhDat ban) {
        int slKhop = Math.min(mua.getSoLuong(), ban.getSoLuong());
        double giaKhop = ban.getGia();
        BigDecimal tien = BigDecimal.valueOf(slKhop * giaKhop);
        double giaTC = lichSuGiaService.getGiaThamChieuMoiNhat(maCP);

        String maNguoiMua = mua.getTaiKhoanNganHang().getMaTK();
        String maNguoiBan = ban.getTaiKhoanNganHang().getMaTK();
        String maNDTMua = mua.getTaiKhoanNganHang().getNhaDauTu().getMaNDT();
        String maNDTBan = ban.getTaiKhoanNganHang().getNhaDauTu().getMaNDT();

        if (!taiKhoanNganHangService.truTien(maNguoiMua, tien)) return;
        if (!soHuuService.giamSoHuu(maNDTBan, maCP, slKhop)) {
            taiKhoanNganHangService.congTien(maNguoiMua, tien);
            return;
        }
        
        if (soHuuService.getSoLuong(maNDTBan, maCP) == 0) {
            soHuuService.xoaSoHuu(maNDTBan, maCP);
        }

        taiKhoanNganHangService.congTien(maNguoiBan, tien);
        soHuuService.tangSoHuu(maNDTMua, maCP, slKhop);

        lenhKhopRepo.save(LenhKhop.builder()
            .lenhDat(mua)
            .ngayGioKhop(LocalDateTime.now())
            .soLuongKhop(slKhop)
            .giaKhop(giaKhop)
            .kieuKhop(mua.getSoLuong() == slKhop ? "Khớp hết" : "Khớp 1 phần")
            .build());

        lenhKhopRepo.save(LenhKhop.builder()
            .lenhDat(ban)
            .ngayGioKhop(LocalDateTime.now())
            .soLuongKhop(slKhop)
            .giaKhop(giaKhop)
            .kieuKhop(ban.getSoLuong() == slKhop ? "Khớp hết" : "Khớp 1 phần")
            .build());

        capNhatTrangThai(mua, slKhop);
        capNhatTrangThai(ban, slKhop);



        messagingTemplate.convertAndSend("/topic/stock-board",
                createOrderMessage(maCP, giaKhop, slKhop, giaTC, mua.getGia(), ban.getGia()));


    }

    private void capNhatTrangThai(LenhDat lenh, int slKhop) {
        lenh.setSoLuong(lenh.getSoLuong() - slKhop);
        lenh.setTrangThai(lenh.getSoLuong() == 0 ? "Hết" : "Một phần");
        lenhDatRepo.save(lenh);
    }

    private Object createOrderMessage(
            String maCPinput,
            double giakhop,
            int soLuongkhop,
            double giaThamChieu,
            double giamua,
            double giaban
    ) {
        return new Object() {
            public String type = "match";
            public String maCP = maCPinput;
            public double giaKhop = giakhop;
            public int soLuongKhop = soLuongkhop;
            public double delta = giaKhop - giaThamChieu;
            public double giaMua = giamua;
            public double giaBan = giaban;
        };
    }

}