package chungkhoan.dto;

import chungkhoan.entity.NganHang;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaiKhoanNganHangTemp {
    private String maTK;
    private NhaDauTu nhaDauTu;
    private NganHang nganHang;
    private BigDecimal soTien;
    private boolean daXoa;

    public TaiKhoanNganHangTemp(TaiKhoanNganHang tknh) {
        this.maTK = tknh.getMaTK();
        this.nhaDauTu = tknh.getNhaDauTu();
        this.nganHang = tknh.getNganHang();
        this.soTien = tknh.getSoTien();
        this.daXoa = false;
    }

    public TaiKhoanNganHang toTaiKhoanNganHangEntity() {
        TaiKhoanNganHang tknh = new TaiKhoanNganHang();
        tknh.setMaTK(this.maTK);
        tknh.setNhaDauTu(this.nhaDauTu);
        tknh.setNganHang(this.nganHang != null ? this.nganHang : new NganHang());
        tknh.setSoTien(this.soTien);
        return tknh;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaiKhoanNganHangTemp)) return false;
        TaiKhoanNganHangTemp that = (TaiKhoanNganHangTemp) o;
        return maTK != null && maTK.equals(that.getMaTK());
    }

    @Override
    public int hashCode() {
        return maTK != null ? maTK.hashCode() : 0;
    }
}