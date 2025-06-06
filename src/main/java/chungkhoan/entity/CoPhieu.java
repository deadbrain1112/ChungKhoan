package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "cophieu")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoPhieu {
    @Id
    @Column(name = "MaCP", columnDefinition = "VARCHAR(10)")
    private String maCP;

    @Column(name = "tencty", columnDefinition = "NVARCHAR(50)", nullable = false, unique = true)
    private String tenCty;

    @Column(name = "diachi", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String diaChi;

    @Column(name = "soluongph", nullable = false)
    private int soLuongPH;

    @OneToMany(mappedBy = "coPhieu", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<SoHuu> soHuus;

    @PrePersist
    @PreUpdate
    private void validateSoLuongPH() {
        if (this.soLuongPH <= 0) {
            throw new IllegalArgumentException("Số lượng cổ phiếu phát hành phải lớn hơn 0!");
        }
    }
    
    public CoPhieu(CoPhieu other) {
        this.maCP = other.maCP;
        this.tenCty = other.tenCty;
        this.diaChi = other.diaChi;
        this.soLuongPH = other.soLuongPH;
    }
}
