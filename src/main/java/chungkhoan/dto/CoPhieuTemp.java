package chungkhoan.dto;

import chungkhoan.entity.CoPhieu;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoPhieuTemp {
    private String maCP;
    private String tenCty;
    private String diaChi;
    private int soLuongPH;
    private boolean daXoa = false;

    public CoPhieuTemp(CoPhieu cp) {
        this.maCP = cp.getMaCP();
        this.tenCty = cp.getTenCty();
        this.diaChi = cp.getDiaChi();
        this.soLuongPH = cp.getSoLuongPH();
        this.daXoa = false;
    }

    public CoPhieu toCoPhieuEntity() {
        return CoPhieu.builder()
                .maCP(maCP)
                .tenCty(tenCty)
                .diaChi(diaChi)
                .soLuongPH(soLuongPH)
                .build();
    }
}
