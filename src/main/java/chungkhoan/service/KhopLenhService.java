package chungkhoan.service;

import chungkhoan.entity.LenhDat;
import chungkhoan.repository.LenhDatRepository;
import chungkhoan.util.TradingTimeUtil;
import chungkhoan.util.TradingTimeUtil.Phase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KhopLenhService implements ApplicationContextAware {

    private final LenhDatRepository lenhDatRepo;
    private final KhopLenhProcessorService processorService;
    private final TradingTimeUtil tradingTimeUtil;

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
        huyLenhATXChuaKhop();
    }

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void khopLORealtime() {
        LocalDateTime now = LocalDateTime.now();
        Phase phase = tradingTimeUtil.getCurrentPhase(now);

        if (phase == Phase.LO) {
            List<String> dsMaCP = lenhDatRepo.findAllMaCPDangChoKhop("Chờ");
            for (String maCP : dsMaCP) {
                System.out.println("[KHOP LO] Mã CP: " + maCP);
                processorService.khopLenh(maCP, Phase.LO.name());
            }
        }
    }

    private void xuLyKhopTheoLoai(Phase phase) {
        List<String> dsMaCP = lenhDatRepo.findAllMaCPDangChoKhop("Chờ");
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
}