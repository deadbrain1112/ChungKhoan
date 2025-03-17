package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "ndt")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhaDauTu {
    @Id
    @Column(name = "MaNDT", columnDefinition = "NCHAR(20)")
    private String maNDT;

    @Column(name = "hoten", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String hoTen;

    @Column(name = "ngaysinh")
    private LocalDate ngaySinh;

    @Column(name = "MKGD", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String mkGiaoDich;

    @Column(name = "diachi", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String diaChi;

    @Column(name = "Phone", columnDefinition = "NVARCHAR(15)", nullable = false)
    private String phone;

    @Column(name = "CMND", columnDefinition = "NCHAR(10)", unique = true, nullable = false)
    private String cmnd;

    @Column(name = "gioitinh", columnDefinition = "NCHAR(5)")
    private String gioiTinh;

    @Column(name = "Email", columnDefinition = "NVARCHAR(50)")
    private String email;
}
