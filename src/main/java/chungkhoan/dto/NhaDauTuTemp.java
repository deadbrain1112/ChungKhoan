package chungkhoan.dto;

import chungkhoan.entity.NhaDauTu;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhaDauTuTemp {
    private String maNDT;
    private String hoTen;
    private LocalDate ngaySinh;
    private String diaChi;
    private String phone;
    private String cmnd;
    private String gioiTinh;
    private String email;
    private boolean daXoa = false;

    public NhaDauTuTemp(NhaDauTu ndt) {
        this.maNDT = ndt.getMaNDT();
        this.hoTen = ndt.getHoTen();
        this.ngaySinh = ndt.getNgaySinh();
        this.diaChi = ndt.getDiaChi();
        this.phone = ndt.getPhone();
        this.cmnd = ndt.getCmnd();
        this.gioiTinh = ndt.getGioiTinh();
        this.email = ndt.getEmail();
        this.daXoa = false;
    }

    public NhaDauTu toNhaDauTuEntity() {
        return NhaDauTu.builder()
                .maNDT(maNDT)
                .hoTen(hoTen)
                .ngaySinh(ngaySinh)
                .diaChi(diaChi)
                .phone(phone)
                .cmnd(cmnd)
                .gioiTinh(gioiTinh)
                .email(email)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NhaDauTuTemp)) return false;
        NhaDauTuTemp that = (NhaDauTuTemp) o;
        return maNDT != null && maNDT.equals(that.getMaNDT());
    }

    @Override
    public int hashCode() {
        return maNDT != null ? maNDT.hashCode() : 0;
    }
}