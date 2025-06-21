package chungkhoan.service;

import chungkhoan.entity.LenhDat;
import chungkhoan.entity.LenhKhop;
import chungkhoan.repository.CoPhieuRepository;
import chungkhoan.repository.LenhDatRepository;
import chungkhoan.repository.LenhKhopRepository;
import chungkhoan.util.TradingTimeUtil;
import chungkhoan.util.TradingTimeUtil.Phase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class KhopLenhService implements ApplicationContextAware {

    private final LenhDatRepository lenhDatRepo;
    private final KhopLenhProcessorService processorService;
    private final TradingTimeUtil tradingTimeUtil;
    private final LenhKhopRepository lenhKhopRepository;
    private final CoPhieuRepository coPhieuRepository;
    private final LichSuGiaService lichSuGiaService;
    
    private final Map<String, List<Object[]>> topMuaSnapshotMap = new ConcurrentHashMap<>();
    private final Map<String, List<Object[]>> topBanSnapshotMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unused")
	private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        this.context = ctx;
    }

    @Scheduled(cron = "${trading-time.cron-ato-check}")
    @Transactional
    public void xuLyKhopLenhSauATO() {
        System.out.println("[KHOP SAU ATO] Xử lý khớp ATO.");
        xuLyKhopTheoLoai(Phase.ATO);
    }

    @Scheduled(cron = "${trading-time.cron-atc-check}")
    @Transactional
    public void xuLyKhopLenhSauATC() {
        System.out.println("[KHOP SAU ATC] Xử lý khớp ATC.");
        xuLyKhopTheoLoai(Phase.ATC);
        snapshotBangGiaCuoiNgay();
        huyLenhATXChuaKhop();
        huyLenhLOChuaKhop();
    }
    
    @Transactional
    public void snapshotBangGiaCuoiNgay() {
        LocalDate ngay = LocalDate.now();
        String ngayStr = ngay.toString(); // yyyy-MM-dd
        Timestamp thoiGianSnapshot = Timestamp.valueOf(LocalDateTime.now());

        List<String> dsMaCP = coPhieuRepository.findAllMaCP();

        for (String maCP : dsMaCP) {
            // 1. Lấy lệnh khớp cuối
            LenhKhop khopCuoi = lenhKhopRepository.findLenhKhopCuoiTrongNgay(maCP, ngayStr);
            if (khopCuoi == null) {
                System.out.println("[SNAPSHOT] Không có khớp nào cho mã: " + maCP);
                continue;
            }

            double giaTC = khopCuoi.getGiaKhop();

            // 2. Lưu lịch sử giá
            lichSuGiaService.snapshotLichSuGia(maCP, thoiGianSnapshot, giaTC, true);

            // 3. Top 3 MUA và BÁN (chỉ lưu vào RAM)
            List<Object[]> topMua = lenhDatRepo.findTop3GiaMuaSnapshot(maCP, ngayStr);
            List<Object[]> topBan = lenhDatRepo.findTop3GiaBanSnapshot(maCP, ngayStr);
            topMuaSnapshotMap.put(maCP, topMua);
            topBanSnapshotMap.put(maCP, topBan);
        }

        System.out.println("[SNAPSHOT] Hoàn tất snapshot bảng giá cuối phiên ATC.");
    }
    
    // Truy xuất giá trị tạm thời trong phase NGHI
    public List<Object[]> getTopMuaSnapshot(String maCP) {
        return topMuaSnapshotMap.getOrDefault(maCP, List.of());
    }

    public List<Object[]> getTopBanSnapshot(String maCP) {
        return topBanSnapshotMap.getOrDefault(maCP, List.of());
    }

    //@Scheduled(fixedDelay = 3000)
    @Transactional
    public void khopLORealtime() {
        LocalDateTime now = LocalDateTime.now();
        Phase phase = tradingTimeUtil.getCurrentPhase(now);

        if (phase == Phase.LO) {
        	List<String> dsMaCP = lenhDatRepo.findMaCPCoTheKhop("LO", "Chờ", "Một phần");
            for (String maCP : dsMaCP) {
                processorService.khopLenh(maCP, Phase.LO.name());
            }
        }
    }

    private void xuLyKhopTheoLoai(Phase phase) {
    	List<String> dsMaCP = lenhDatRepo.findMaCPCoTheKhop(phase.name(), "Chờ", "Một phần");

        for (String maCP : dsMaCP) {
            System.out.println("[KHOP " + phase.name() + "] Mã CP: " + maCP);
            processorService.khopLenh(maCP, phase.name());
        }
    }

    @Transactional
    public void huyLenhATXChuaKhop() {
        List<LenhDat> lenhATX = lenhDatRepo.findByLoaiLenhInAndTrangThai(List.of("ATC", "ATO"), "Chờ");
        for (LenhDat lenh : lenhATX) {
            lenh.setTrangThai("Hủy");
            lenhDatRepo.save(lenh);
        }
        if (!lenhATX.isEmpty()) {
            System.out.println("[HUY ATX] Đã hủy " + lenhATX.size() + " lệnh ATO/ATC chưa khớp sau giờ.");
        }
    }
    
    @Transactional
    public void huyLenhLOChuaKhop() {
        List<LenhDat> lenhLO = lenhDatRepo.findByLoaiLenhInAndTrangThai(List.of("LO"), "Chờ");
        for (LenhDat lenh : lenhLO) {
            lenh.setTrangThai("Hủy");
            lenhDatRepo.save(lenh);
        }
        if (!lenhLO.isEmpty()) {
            System.out.println("[HUY LO] Đã hủy " + lenhLO.size() + " lệnh LO chưa khớp sau phiên.");
        }
    }
}