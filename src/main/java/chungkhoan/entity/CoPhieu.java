package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @PrePersist
    @PreUpdate
    private void validateSoLuongPH() {
        if (this.soLuongPH <= 0) {
            throw new IllegalArgumentException("Số lượng cổ phiếu phát hành phải lớn hơn 0!");
        }
    }
}
