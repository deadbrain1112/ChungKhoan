package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sohuu")
@IdClass(SoHuuKey.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoHuu {
    @Id
    @Column(name = "MaNDT", columnDefinition = "NCHAR(20)")
    private String maNDT;

    @Id
    @Column(name = "MaCP", columnDefinition = "NCHAR(10)")
    private String maCP;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaNDT", insertable = false, updatable = false)
    private NhaDauTu nhaDauTu;  // Mối quan hệ với Nhà Đầu Tư

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaCP", insertable = false, updatable = false)
    private CoPhieu coPhieu;  // Mối quan hệ với Cổ Phiếu

    @Column(name = "SoLuong", nullable = false)
    private int soLuong;
}
