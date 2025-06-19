package chungkhoan.dto;

import chungkhoan.entity.NhanVien;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhanVienTemp {
    private String maNV;
    private String hoTen;
    private LocalDate ngaySinh;
    private String diaChi;
    private String phone;
    private String cmnd;
    private String gioiTinh;
    private String email;
    private boolean daXoa = false;

    public NhanVienTemp(NhanVien nv) {
        this.maNV = nv.getMaNV();
        this.hoTen = nv.getHoTen();
        this.ngaySinh = nv.getNgaySinh();
        this.diaChi = nv.getDiaChi();
        this.phone = nv.getPhone();
        this.cmnd = nv.getCmnd();
        this.gioiTinh = nv.getGioiTinh();
        this.email = nv.getEmail();
        this.daXoa = false;
    }

    public NhanVien toNhanVienEntity() {
        return NhanVien.builder()
                .maNV(maNV)
                .hoTen(hoTen)
                .ngaySinh(ngaySinh)
                .diaChi(diaChi)
                .phone(phone)
                .cmnd(cmnd)
                .gioiTinh(gioiTinh)
                .email(email)
                .build();
    }
}
