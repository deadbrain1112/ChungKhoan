package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lichsugia")
@IdClass(LichSuGiaKey.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichSuGia {
    @Id
    @ManyToOne
    @JoinColumn(name = "MaCP", nullable = false,columnDefinition = "NCHAR(10)")
    private CoPhieu coPhieu;

    @Id
    @Column(name = "Ngay", nullable = false)
    private LocalDateTime ngay;

    @Column(name = "giatran", nullable = false)
    private double giaTran;

    @Column(name = "giasan", nullable = false)
    private double giaSan;

    @Column(name = "GiaTC", nullable = false)
    private double giaTC;
}
