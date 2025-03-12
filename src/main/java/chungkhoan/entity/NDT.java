package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "NDT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NDT {
    @Id
    @Column(length = 20)
    private String maNDT;

    @Column(nullable = false, length = 50)
    private String hoTen;

    private LocalDate ngaySinh;

    @Column(nullable = false, length = 50)
    private String mkGiaoDich;

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

    @OneToOne(mappedBy = "ndt")
    private User user;

    @OneToMany(mappedBy = "ndt")
    private List<TaiKhoanNganHang> taiKhoanNganHangs;

    @OneToMany(mappedBy = "ndt")
    private List<SoHuu> soHuus;
}
