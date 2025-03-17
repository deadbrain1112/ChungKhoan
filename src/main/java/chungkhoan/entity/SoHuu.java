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
    @ManyToOne
    @JoinColumn(name = "MaNDT", nullable = false, columnDefinition = "NCHAR(20)")
    private NhaDauTu nhaDauTu;

    @Id
    @ManyToOne
    @JoinColumn(name = "MaCP", nullable = false,columnDefinition = "NCHAR(10)")
    private CoPhieu coPhieu;

    @Column(name = "soluong", nullable = false)
    private int soLuong;
}
