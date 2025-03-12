package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LENHDAT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LenhDat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long maGD;

    private LocalDateTime ngayGD;

    @Column(length = 1)
    private String loaiGD; // Mua (M) hoặc Bán (B)

    @Column(length = 5)
    private String loaiLenh; // LO, ATO, ATC

    private int soLuong;

    @ManyToOne
    @JoinColumn(name = "maCP", nullable = false)
    private CoPhieu coPhieu;

    private double gia;

    @ManyToOne
    @JoinColumn(name = "maTK", nullable = false)
    private TaiKhoanNganHang taiKhoan;

    @Column(length = 20)
    private String trangThai;
}
