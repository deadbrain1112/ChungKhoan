package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "COPHIEU")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoPhieu {
    @Id
    @Column(length = 10, nullable = false)
    private String maCP;

    @Column(nullable = false, unique = true, length = 50, columnDefinition = "nvarchar(50)")
    private String tenCty;

    @Column(nullable = false, length = 100, columnDefinition = "nvarchar(100)")
    private String diaChi;

    @Column(nullable = false)
    private int soLuongPH;
}