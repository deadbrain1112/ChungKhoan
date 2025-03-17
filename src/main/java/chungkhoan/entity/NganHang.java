package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nganhang")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NganHang {
    @Id
    @Column(name = "MaNH", columnDefinition = "NCHAR(20)")
    private String maNH;

    @Column(name = "TenNH", columnDefinition = "NVARCHAR(50)", unique = true, nullable = false)
    private String tenNH;

    @Column(name = "diachi", columnDefinition = "NVARCHAR(100)")
    private String diaChi;

    @Column(name = "Phone", columnDefinition = "NCHAR(10)")
    private String phone;

    @Column(name = "Email", columnDefinition = "NVARCHAR(50)")
    private String email;
}
