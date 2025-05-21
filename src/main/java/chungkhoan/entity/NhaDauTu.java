package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "ndt")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhaDauTu {
    @Id
    @Column(name = "MaNDT", columnDefinition = "NCHAR(20)")
    private String maNDT;

    @Column(name = "hoten", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String hoTen;

    @Column(name = "ngaysinh")
    private LocalDate ngaySinh;

    @Column(name = "MKGD", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String mkGiaoDich;

    @Column(name = "diachi", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String diaChi;

    @Column(name = "Phone", columnDefinition = "NVARCHAR(15)", nullable = false)
    private String phone;

    @Column(name = "CMND", columnDefinition = "NCHAR(10)", unique = true, nullable = false)
    private String cmnd;

    @Column(name = "gioitinh", columnDefinition = "NCHAR(5)")
    private String gioiTinh;

    @Column(name = "Email", columnDefinition = "NVARCHAR(50)")
    private String email;

    @OneToMany(mappedBy = "nhaDauTu", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<SoHuu> soHuus;

    @OneToMany(mappedBy = "nhaDauTu", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TaiKhoanNganHang> taiKhoanNganHangs = new ArrayList<>();

    public NhaDauTu(NhaDauTu other) {
        this.maNDT = other.maNDT;
        this.hoTen = other.hoTen;
        this.ngaySinh = other.ngaySinh;
        this.mkGiaoDich = other.mkGiaoDich;
        this.diaChi = other.diaChi;
        this.phone = other.phone;
        this.cmnd = other.cmnd;
        this.gioiTinh = other.gioiTinh;
        this.email = other.email;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NhaDauTu)) return false;
        NhaDauTu that = (NhaDauTu) o;
        return maNDT != null && maNDT.equals(that.getMaNDT());
    }

    @Override
    public int hashCode() {
        return maNDT != null ? maNDT.hashCode() : 0;
    }


}
