package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "NHANVIEN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhanVien {
    @Id
    @Column(length = 20)
    private String maNV;

    @Column(nullable = false, length = 50)
    private String hoTen;

    private LocalDate ngaySinh;

    @Column(nullable = false, length = 100)
    private String diaChi;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false, unique = true, length = 10)
    private String cmnd;

    @Column(length = 5)
    private String gioiTinh;

    @Column(length = 50)
    private String email;

    @OneToOne(mappedBy = "nhanVien")
    private User user;
}
