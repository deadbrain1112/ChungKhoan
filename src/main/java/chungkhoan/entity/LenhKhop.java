package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LENHKHOP")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LenhKhop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long maLK;

    @ManyToOne
    @JoinColumn(name = "maGD", nullable = false)
    private LenhDat lenhDat;

    private LocalDateTime ngayGioKhop;
    private int soLuongKhop;
    private double giaKhop;

    @Column(length = 50)
    private String kieuKhop;
}
