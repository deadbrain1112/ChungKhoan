package chungkhoan.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LichSuGiaKey implements Serializable {
    private String coPhieu;
    private LocalDateTime ngay;
}
