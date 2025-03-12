package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "COPHIEU")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoPhieu {
    @Id
    @Column(length = 10)
    private String maCP;

    @Column(nullable = false, unique = true, length = 50)
    private String tenCty;

    @Column(nullable = false, length = 100)
    private String diaChi;

    private int soLuongPH;
}
