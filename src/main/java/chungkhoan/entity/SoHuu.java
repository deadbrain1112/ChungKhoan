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

    @Column(name = "SoLuong", nullable = false)
    private int soLuong;
}
