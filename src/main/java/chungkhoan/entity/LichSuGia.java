package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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
    private LocalDate ngay;

    @Column(nullable = false)
    private double giaTran;

    @Column(nullable = false)
    private double giaSan;

    @Column(nullable = false)
    private double giaTC;
}