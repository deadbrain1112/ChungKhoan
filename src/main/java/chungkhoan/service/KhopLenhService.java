package chungkhoan.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import chungkhoan.entity.LenhDat;
import chungkhoan.repository.LenhDatRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KhopLenhService implements ApplicationContextAware {

    private final LenhDatRepository lenhDatRepo;
    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        this.context = ctx;
    }

    private boolean isTrongGioGiaoDich() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
            return false;

        boolean sang = !time.isBefore(LocalTime.of(6, 0)) && time.isBefore(LocalTime.of(12, 0));
        boolean chieu = !time.isBefore(LocalTime.of(13, 0)) && time.isBefore(LocalTime.of(16, 0));
        return sang || chieu;
    }

    @Scheduled(fixedDelay = 1000)
    public void khopLenhTuDong() {
        if (!isTrongGioGiaoDich()) {
            huyLenhHetGio();  
            return;
        }

        List<String> dsMaCP = lenhDatRepo.findAllMaCPDangChoKhop("Chờ");

        KhopLenhProcessorService processor = context.getBean(KhopLenhProcessorService.class);

        for (String maCP : dsMaCP) {
            processor.khopLenh(maCP);
        }
    }

    @Transactional
    public void huyLenhHetGio() {
        List<LenhDat> lenhCho = lenhDatRepo.findByTrangThai("Chờ");

        int huyCount = 0;
        LocalTime time = LocalTime.now();

        for (LenhDat lenh : List.copyOf(lenhCho)) {
            LocalTime gioLenh = lenh.getNgayGD().toLocalTime();

            // Hủy những lệnh đặt sau phiên giao dịch liên tục chưa được khớp
            boolean trongGioLienTuc = 
                (!gioLenh.isBefore(LocalTime.of(6, 0)) && gioLenh.isBefore(LocalTime.of(12, 0))) ||
                (!gioLenh.isBefore(LocalTime.of(13, 0)) && gioLenh.isBefore(LocalTime.of(16, 0)));

            if (trongGioLienTuc) {
                lenh.setTrangThai("Hủy");
                lenhDatRepo.save(lenh);
                huyCount++;
            }
        }

        if (huyCount > 0) {
            System.out.println("[HUY LENH] Đã hủy " + huyCount + " lệnh 'Chờ' được đặt trong giờ giao dịch liên tục.");
        }
    }
}
