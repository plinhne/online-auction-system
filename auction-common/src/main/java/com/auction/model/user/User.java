package com.auction.model.user;
import com.auction.model.base.Entity;

public abstract class User extends Entity {

    private int id;
    private String name;
    private String email;
    private String password;
    private UserRole role;      // dùng enum
    private UserStatus status;  // thêm status
    private double walletBalance;

    /*Serialization Version Unique Identifier
    ** (Mã định danh phiên bản tuần tự hóa)
    * 1L là 1 long
    * Nếu implements Serializable mà không viết dòng này,
    * Java sẽ tự động tạo ra một cái ID ngầm định phức tạp
    * (dựa trên tên class, các biến, các hàm có bên trong).
    */
    private static final long serialVersionUID = 1L;


    public User(int id, String name, String email, String password, UserRole role) {
        super(id);
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }
    public User(int id, String name, double walletBalance) {
        super(id);
        this.id = id;
        this.name = name;
        this.walletBalance = walletBalance;
    }

//getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public UserStatus getStatus() {
        return status;
    }
    public UserRole getRole() {
        return role;
    }
    public double getWalletBalance() {
        return walletBalance;
    }

    //setters
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setWalletBalance(double walletBalance){ this.walletBalance = walletBalance;}

    public void depositMoney(double amount) {walletBalance += amount;} // nạp tiền

    // trừ tiền
    public void withdrawMoney(double amount) {

        if (walletBalance < amount) {
            throw new RuntimeException("Not enough balance");
        }

        walletBalance -= amount;
    }

}