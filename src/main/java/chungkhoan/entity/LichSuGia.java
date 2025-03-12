package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LICHSUGIA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichSuGia {
    @Id
    @ManyToOne
    @JoinColumn(name = "maCP", nullable = false)
    private CoPhieu coPhieu;

    @Id
    private LocalDateTime ngay;

    private double giaTran;
    private double giaSan;
    private double giaTC;
}
