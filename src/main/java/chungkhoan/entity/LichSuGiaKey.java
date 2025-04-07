package chungkhoan.entity;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LichSuGiaKey implements Serializable {
    private String maCP;
    private Timestamp ngay; // Dùng Timestamp thay vì LocalDateTime để tránh lỗi khi mapping với SQL Server

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LichSuGiaKey that = (LichSuGiaKey) o;
        return Objects.equals(maCP, that.maCP) && Objects.equals(ngay, that.ngay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maCP, ngay);
    }
}
