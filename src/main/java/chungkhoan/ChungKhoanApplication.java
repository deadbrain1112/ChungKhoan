package chungkhoan;

import chungkhoan.service.DatabaseService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import java.util.Scanner;

@SpringBootApplication
public class ChungKhoanApplication {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(ChungKhoanApplication.class, args);
        DatabaseService databaseService = context.getBean(DatabaseService.class);

        Scanner scanner = new Scanner(System.in);
        System.out.print("Nhập tên đăng nhập: ");
        String username = scanner.nextLine();
        System.out.print("Nhập mật khẩu: ");
        String password = scanner.nextLine();

        boolean success = databaseService.testConnection(username, password);
        if (success) {
            System.out.println("Đăng nhập thành công");
        } else {
            System.out.println("Không thể đăng nhập");
        }
    }
}