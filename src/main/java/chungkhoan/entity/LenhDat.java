package chungkhoan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lenhdat")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LenhDat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaGD")
    private Long maGD;

    @Column(name = "NgayGD", nullable = false, columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime ngayGD;

    @Column(name = "LoaiGD", columnDefinition = "CHAR(1) CHECK (LoaiGD IN ('M', 'B'))", nullable = false)
    private String loaiGD;

    @Column(name = "loailenh", columnDefinition = "NCHAR(5)", nullable = false)
    private String loaiLenh;

    @Column(name = "soluong", nullable = false)
    private int soLuong;

    @ManyToOne
    @JoinColumn(name = "MaCP", nullable = false, columnDefinition = "NCHAR(10)")
    private CoPhieu coPhieu;

    @Column(name = "Gia", nullable = false)
    private double gia;

    @ManyToOne
    @JoinColumn(name = "MaTK", nullable = false)
    private TaiKhoanNganHang taiKhoanNganHang;

    @Column(name = "trangthai", columnDefinition = "NVARCHAR(20)", nullable = false)
    private String trangThai;

    // Đảm bảo số lượng và giá đặt hợp lệ
    @PrePersist
    @PreUpdate
    private void validateLenhDat() {
        if (this.soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng cổ phiếu đặt phải lớn hơn 0!");
        }
        if (this.gia <= 0) {
            throw new IllegalArgumentException("Giá đặt phải lớn hơn 0!");
        }
        if (!this.trangThai.matches("Hủy|Chưa|Một phần|Hết|Chờ")) {
            throw new IllegalArgumentException("Trạng thái lệnh không hợp lệ!");
        }
    }
}
