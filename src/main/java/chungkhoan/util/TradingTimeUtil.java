package chungkhoan.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class TradingTimeUtil {

    private final TradingTimeProperties config;

    public enum Phase {
        ATO, LO, ATC, NGHI
    }

    public Phase getCurrentPhase(LocalDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        System.out.println("[DEBUG] Giờ hiện tại: " + time);
        System.out.println("[DEBUG] ATO: " + config.getAtoStart() + " - " + config.getAtoEnd());
        System.out.println("[DEBUG] LO:  " + config.getAtoEnd() + " - " + config.getLoEnd());
        System.out.println("[DEBUG] ATC: " + config.getLoEnd() + " - " + config.getAtcEnd());

//        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
//            System.out.println("→ Phase = NGHI (thứ Bảy/CN)");
//            return Phase.NGHI;
//        }

        if (!time.isBefore(config.getAtoStart()) && time.isBefore(config.getAtoEnd())) {
            System.out.println("→ Phase = ATO");
            return Phase.ATO;
        }

        if (!time.isBefore(config.getAtoEnd()) && time.isBefore(config.getLoEnd())) {
            System.out.println("→ Phase = LO");
            return Phase.LO;
        }

        if (!time.isBefore(config.getLoEnd()) && time.isBefore(config.getAtcEnd())) {
            System.out.println("→ Phase = ATC");
            return Phase.ATC;
        }

        System.out.println("→ Phase = NGHI (ngoài mọi khung)");
        return Phase.NGHI;
    }


    public boolean isTrongGioGiaoDich(LocalDateTime thoiDiem) {
        if (thoiDiem == null) return false;

        DayOfWeek day = thoiDiem.getDayOfWeek();
        LocalTime time = thoiDiem.toLocalTime();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return false;

        return (!time.isBefore(config.getAtoStart()) && time.isBefore(config.getAtcEnd()));
    }
}
