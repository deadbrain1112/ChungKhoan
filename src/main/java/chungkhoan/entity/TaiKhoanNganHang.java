package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "taikhoan_nganhang")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaiKhoanNganHang {
    @Id
    @Column(name = "MaTK", columnDefinition = "NCHAR(20)")
    private String maTK;

    @ManyToOne
    @JoinColumn(name = "MaNDT", nullable = false)
    private NhaDauTu nhaDauTu;

    @ManyToOne
    @JoinColumn(name = "MaNH", nullable = false)
    private NganHang nganHang;

    @Column(name = "sotien", nullable = false, precision = 18, scale = 2)
    private BigDecimal soTien;
}
