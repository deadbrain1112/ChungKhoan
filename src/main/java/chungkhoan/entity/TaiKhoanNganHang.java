package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TAIKHOAN_NGANHANG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaiKhoanNganHang {
    @Id
    @Column(length = 20)
    private String maTK;

    @ManyToOne
    @JoinColumn(name = "maNDT", nullable = false)
    private NDT ndt;

    private double soTien;

    @ManyToOne
    @JoinColumn(name = "maNH", nullable = false)
    private NganHang nganHang;
}
