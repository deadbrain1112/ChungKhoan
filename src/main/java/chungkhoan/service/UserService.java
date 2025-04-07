package chungkhoan.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private DatabaseService databaseService;

    public boolean authenticateUser(String username, String password, HttpSession session) {
        boolean isValid = databaseService.testConnection(username, password);
        if (isValid) {
            session.setAttribute("username", username);
        }
        return isValid;
    }
}
