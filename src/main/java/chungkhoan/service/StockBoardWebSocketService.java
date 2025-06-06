package chungkhoan.service;

import chungkhoan.entity.*;
import chungkhoan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StockBoardWebSocketService {

    private final CoPhieuRepository coPhieuRepo;
    private final LenhKhopRepository lenhKhopRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public void guiCapNhatBangGia(String maCP) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        CoPhieu cp = coPhieuRepo.findById(maCP).orElse(null);
        if (cp == null) return;

        List<LenhKhop> latestKhops = lenhKhopRepo.findTopByLenhDat_CoPhieuOrderByNgayGioKhopDesc(cp, start, end);
        LenhKhop latestKhop = latestKhops.isEmpty() ? null : latestKhops.get(0);

        Long tongKL = lenhKhopRepo.sumSoLuongKhopByCoPhieu(cp, start, end);

        Map<String, Object> cpData = new HashMap<>();
        cpData.put("maCP", cp.getMaCP());
        cpData.put("gia", latestKhop != null ? latestKhop.getGiaKhop() : 0);
        cpData.put("soLuong", tongKL != null ? tongKL : 0);

        messagingTemplate.convertAndSend("/topic/stock-board" + maCP, cpData);
    }
}
