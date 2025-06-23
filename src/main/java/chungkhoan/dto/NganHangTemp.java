package chungkhoan.dto;

import chungkhoan.entity.NganHang;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NganHangTemp {
    private String maNH;
    private String tenNH;
    private String diaChi;
    private String phone;
    private String email;
    private boolean daXoa = false;

    public NganHangTemp(NganHang nh) {
        this.maNH = nh.getMaNH();
        this.tenNH = nh.getTenNH();
        this.diaChi = nh.getDiaChi();
        this.phone = nh.getPhone();
        this.email = nh.getEmail();
        this.daXoa = false;
    }

    public NganHang toNganHangEntity() {
        return NganHang.builder()
                .maNH(maNH)
                .tenNH(tenNH)
                .diaChi(diaChi)
                .phone(phone)
                .email(email)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NganHangTemp)) return false;
        NganHangTemp that = (NganHangTemp) o;
        return maNH != null && maNH.equals(that.getMaNH());
    }

    @Override
    public int hashCode() {
        return maNH != null ? maNH.hashCode() : 0;
    }
}