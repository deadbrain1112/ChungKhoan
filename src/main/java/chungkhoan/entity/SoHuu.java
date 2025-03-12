package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SOHUU")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoHuu {
    @Id
    @ManyToOne
    @JoinColumn(name = "maNDT", nullable = false)
    private NDT ndt;

    @Id
    @ManyToOne
    @JoinColumn(name = "maCP", nullable = false)
    private CoPhieu coPhieu;

    private int soLuong;
}
