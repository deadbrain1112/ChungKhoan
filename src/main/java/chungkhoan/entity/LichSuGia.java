package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "lichsugia")
@IdClass(LichSuGiaKey.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichSuGia {
    @Id
    @Column(name = "MaCP", columnDefinition = "NCHAR(10)")
    private String maCP;

    @Id
    @Temporal(TemporalType.TIMESTAMP)  // Cập nhật annotation
    @Column(name = "Ngay", nullable = false)
    private Timestamp ngay;

    @Column(name = "GiaTran", nullable = false)
    private float giaTran;

    @Column(name = "GiaSan", nullable = false)
    private float giaSan;

    @Column(name = "GiaTC", nullable = false)
    private float giaTC;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaCP", insertable = false, updatable = false)
    private CoPhieu coPhieu;
}
