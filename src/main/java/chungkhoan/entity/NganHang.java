package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "NGANHANG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NganHang {
    @Id
    @Column(length = 20, nullable = false)
    private String maNH;

    @Column(nullable = false, unique = true, length = 50, columnDefinition = "nvarchar(50)")
    private String tenNH;

    @Column(length = 100, columnDefinition = "nvarchar(100)")
    private String diaChi;

    @Column(length = 10)
    private String phone;

    @Column(length = 50)
    private String email;
}