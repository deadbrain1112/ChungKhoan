package chungkhoan.entity;

import java.io.Serializable;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SoHuuKey implements Serializable {
    private String nhaDauTu;
    private String coPhieu;
}
