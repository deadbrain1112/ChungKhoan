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
    @Column(name = "MaCP", columnDefinition = "NCHAR(10)")
    private String maCP;

    @Column(name = "tencty", columnDefinition = "NVARCHAR(50)", nullable = false, unique = true)
    private String tenCty;

    @Column(name = "diachi", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String diaChi;

    @Column(name = "soluongph", nullable = false)
    private int soLuongPH;

    @OneToMany(mappedBy = "coPhieu", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<SoHuu> soHuus;  // Một cổ phiếu có thể thuộc về nhiều nhà đầu tư

    @PrePersist
    @PreUpdate
    private void validateSoLuongPH() {
        if (this.soLuongPH <= 0) {
            throw new IllegalArgumentException("Số lượng cổ phiếu phát hành phải lớn hơn 0!");
        }
    }
}
