package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lenhkhop")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LenhKhop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaLK")
    private Long maLK;

    @ManyToOne
    @JoinColumn(name = "MaGD", nullable = false)
    private LenhDat lenhDat;

    @Column(name = "ngaygiokhop", nullable = false, columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime ngayGioKhop;

    @Column(name = "soluongkhop", nullable = false)
    private int soLuongKhop;

    @Column(name = "giakhop", nullable = false)
    private double giaKhop;

    @Column(name = "kieukhop", columnDefinition = "NVARCHAR(50) CHECK (KieuKhop IN ('Khớp 1 phần', 'Khớp hết'))", nullable = false)
    private String kieuKhop;

    // Kiểm tra dữ liệu trước khi lưu vào DB
    @PrePersist
    @PreUpdate
    private void validateLenhKhop() {
        if (this.soLuongKhop <= 0) {
            throw new IllegalArgumentException("Số lượng khớp phải lớn hơn 0!");
        }
        if (this.giaKhop <= 0) {
            throw new IllegalArgumentException("Giá khớp phải lớn hơn 0!");
        }
        if (!this.kieuKhop.matches("Khớp 1 phần|Khớp hết")) {
            throw new IllegalArgumentException("Kiểu khớp không hợp lệ! Chỉ chấp nhận 'Khớp 1 phần' hoặc 'Khớp hết'.");
        }
    }
}
