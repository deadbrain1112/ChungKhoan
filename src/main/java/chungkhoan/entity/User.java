package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToOne
    @JoinColumn(name = "maNDT", referencedColumnName = "maNDT", unique = true)
    private NDT ndt;

    @OneToOne
    @JoinColumn(name = "maNV", referencedColumnName = "maNV", unique = true)
    private NhanVien nhanVien;
}
