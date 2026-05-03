import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.*;
import java.net.Socket;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMessage;

    // Thông tin kết nối Server
    private final String SERVER_HOST = "localhost";
    private final int SERVER_PORT = 12345;

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            lblMessage.setText("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        // Kết nối Socket và gửi dữ liệu
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Gửi chuỗi định dạng đơn giản (có thể thay bằng JSON như tài liệu yêu cầu)
            out.println("LOGIN:" + username + ":" + password);

            // Nhận phản hồi từ Server
            String response = in.readLine();
            if ("SUCCESS".equals(response)) {
                lblMessage.setStyle("-fx-text-fill: green;");
                lblMessage.setText("Đăng nhập thành công!");
                // Logic chuyển sang màn hình AuctionList.fxml ở đây
            } else {
                lblMessage.setStyle("-fx-text-fill: red;");
                lblMessage.setText("Sai tên đăng nhập hoặc mật khẩu.");
            }

        } catch (IOException e) {
            lblMessage.setText("Lỗi: Không thể kết nối tới máy chủ.");
            e.printStackTrace();
        }
    }
}
