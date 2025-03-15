package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "TAIKHOAN_NGANHANG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaiKhoanNganHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long maTK;

    @ManyToOne
    @JoinColumn(name = "maNDT", nullable = false)
    private NDT ndt;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal soTien;

    @ManyToOne
    @JoinColumn(name = "maNH", nullable = false)
    private NganHang nganHang;
}
