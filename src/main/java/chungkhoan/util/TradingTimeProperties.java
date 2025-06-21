package chungkhoan.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "trading-time")
public class TradingTimeProperties {
    private LocalTime atoStart;
    private LocalTime atoEnd;
    private LocalTime loEnd;
    private LocalTime atcEnd;
}