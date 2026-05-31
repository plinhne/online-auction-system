# Hệ thống Đấu giá Trực tuyến (Online Auction System)
**Bài tập lớn môn: Lập trình nâng cao**  
**Trường: Đại học Công nghệ (VNU-UET)**

## Thành viên nhóm & Phân công
- Commit message GitHub ok
- Đa hình: có implement code, chưa triển khai
- Chưa có Design pattern
- Chức năng chính:
	+ Quản lý người dùng: có profile + giao diện hiển thị, chưa có điều chỉnh/thay đổi thông tin người dung
	+ Chức năng đấu giá: view sản phẩm, chưa đấu giá được, 1 số prototype giao diện (chưa BE)
	+ Xử lý lỗi và ngoại lệ: chưa có
- Kỹ thuật an toàn & concurrency: cài đồng thời an toàn (chưa test)
- Anti sniping (chưa test)
- Client server + MVC
- Làm rõ hơn cơ chế Maven
- Chưa có unit test, CI/CD
- 7 - 9
# Auction System

## 1. Giới thiệu

### 1.1 Mô tả bài toán

**1. Mô tả hệ thống**

Hệ thống **Online Auction System** là ứng dụng đấu giá trực tuyến theo mô hình Client-Server, cho phép người dùng đăng bán, tham gia đấu giá và quản lý phiên đấu giá theo thời gian thực.

**Hệ thống hỗ trợ:**
- Đấu giá thời gian thực (real-time bidding)
- Quản lý người dùng (Admin, Seller, Bidder)
- Quản lý phiên đấu giá và vật phẩm
- Cơ chế chống “sniping” (đặt giá sát giờ kết thúc)
- Thanh toán và xử lý trạng thái đấu giá

### 1.2 Mục tiêu

**Mục tiêu của dự án là:**

Hệ thống Online Auction System được xây dựng nhằm mô phỏng nền tảng đấu giá trực tuyến theo mô hình Client-Server, hỗ trợ người dùng tham gia đấu giá theo thời gian thực, đảm bảo cập nhật giá liên tục và tính minh bạch trong quá trình đấu giá. Hệ thống cũng tự động quản lý phiên đấu giá, phân quyền người dùng và ngăn chặn hành vi đặt giá không công bằng.

---

## 2. Công nghệ sử dụng
- **Java 17+**
- **JavaFX** (Client UI)
- **Socket Programming** (Client-Server communication)
- **Maven** (build tool)
- **Gson** (JSON parsing)
- **SLF4J + Logback** (logging)
- **JUnit 5** (testing)

**Môi trường chạy:**
- Windows / Linux / macOS
- JDK 17 trở lên
- Maven 3.8+

---

## 3. Kiến trúc hệ thống

Hệ thống được xây dựng theo kiến trúc **Client - Server**.

```text
+-------------------------------------------------------------+
|                        auction-common                       |
| (Chứa Core Models, Enums, DTOs, Custom Exceptions, Patterns)|
+-------------------------------------------------------------+
                              ^
                              | (Dependency)
        +---------------------+---------------------+
        |                                           |
+-------+-------------------+         +-------------+-------------+
|      auction-client       | Socket  |      auction-server       |
| (JavaFX UI, Controllers,  | <=====> | (Business Services,       |
|  Network listeners)       |  (TCP)  |  DAOs, Data Source)       |
+---------------------------+         +---------------------------+
``` 
### Client
**Chịu trách nhiệm:**
* Hiển thị giao diện người dùng.
* Gửi yêu cầu đến Server.
* Nhận dữ liệu phản hồi.
* Cập nhật dữ liệu đấu giá thời gian thực.

### Server
**Chịu trách nhiệm:**
* Xử lý nghiệp vụ.
* Quản lý người dùng.
* Quản lý đấu giá.
* Kiểm tra tính hợp lệ của các giao dịch.

### Common Module
**Chứa:**
* Models
* DTOs
* Payment Objects
* Custom Exceptions

*(Dùng chung cho Client và Server).*

---

## 4. Cấu trúc thư mục

```text
online-auction-system/
├── .github/workflows/
│   └── maven.yml                 # CI/CD Tự động hóa build & test
├── auction-client/               # Module Giao diện người dùng (Client)
│   ├── src/main/java/com/auction/client/
│   │   ├── controller/           # Điều hướng hành vi giao diện JavaFX
│   │   │   ├── AddEditItemController.java
│   │   │   ├── AdminPanelController.java
│   │   │   ├── AuctionListViewController.java
│   │   │   ├── BaseController.java
│   │   │   ├── ItemCardController.java
│   │   │   ├── LoginController.java
│   │   │   ├── MainController.java
│   │   │   └── RealTimeBiddingController.java (và các controller khác...)
│   │   ├── network/              # Kết nối socket và lắng nghe luồng dữ liệu từ Server
│   │   │   ├── NetworkService.java
│   │   │   └── ServerListener.java
│   │   ├── util/                 # Lớp tiện ích bổ trợ (Format, validate, log)
│   │   │   ├── DialogUtil.java
│   │   │   ├── FormatterUtil.java
│   │   │   ├── LoggerUtil.java
│   │   │   └── ValidationUtil.java
│   │   ├── AuctionClientApp.java # Điểm chạy ứng dụng JavaFX chính
│   │   └── MainAppLauncher.java  # Khởi chạy không mở rộng Application (Fix classpath)
│   └── src/main/resources/
│       ├── css/style.css         # Định dạng style cho toàn hệ thống
│       └── fxml/                 # Layout UI thiết kế bằng XML
│           ├── components/       # Các mảnh UI nhỏ tái sử dụng
│           ├── AddItemView.fxml
│           ├── AdminPanelView.fxml
│           └── AuctionListView.fxml (và các màn hình view khác...)
│
├── auction-common/               # Module cấu trúc dữ liệu dùng chung (Common)
│   ├── src/main/java/com/auction/
│   │   ├── dto/
│   │   │   └── AuctionDTO.java   # Đối tượng truyền dữ liệu qua mạng
│   │   ├── exception/            # Khởi tạo các ngoại lệ nghiệp vụ hệ thống
│   │   │   ├── AccountLockedException.java
│   │   │   ├── AuctionClosedException.java
│   │   │   ├── InvalidBidException.java
│   │   │   └── ItemNotFoundException.java (và các Exception kiểm soát khác...)
│   │   ├── model/                # Các đối tượng thực thể cốt lõi (Domain Models)
│   │   │   ├── auction/
│   │   │   │   ├── Bid.java
│   │   │   │   └── BidStatus.java (Enum)
│   │   │   ├── item/
│   │   │   │   ├── Item.java, Art.java, Electronics.java, Vehicle.java (Kế thừa OOP)
│   │   │   │   └── ItemStatus.java / ItemCategory.java (Enums)
│   │   │   ├── pattern/          # Áp dụng các mẫu thiết kế (Design Patterns)
│   │   │   │   ├── factory/ItemFactory.java
│   │   │   │   ├── observer/AuctionSubject.java, BidObserver.java
│   │   │   │   └── singleton/AuctionManager.java
│   │   │   ├── user/
│   │   │   │   ├── User.java, Admin.java, Bidder.java, Seller.java
│   │   │   │   └── UserRole.java / UserStatus.java
│   │   │   └── payment/
│   │   │       └── Deposit.java, AuctionResult.java, PaymentStatus.java
│   │   └── network/
│   │       └── NetworkMessage.java # Định dạng gói tin giao tiếp Client-Server
│   └── src/test/java/            # Unit Test cho các thực thể và pattern core
│
├── auction-server/               # Module logic nghiệp vụ phía Máy chủ (Server)
│   ├── src/main/java/com/auction/server/
│   │   ├── config/DatabaseConfig.java
│   │   ├── dao/                  # Lớp trừu tượng hóa truy cập dữ liệu
│   │   │   ├── impl/InMemoryAuctionDAO.java, InMemoryItemDAO.java, InMemoryUserDAO.java
│   │   │   ├── GenericDAO.java
│   │   │   └── UserDAO.java / ItemDAO.java / BidDAO.java
│   │   ├── controller/           # Tiếp nhận và định tuyến gói tin từ client
│   │   │   ├── AuctionController.java
│   │   │   └── AuthController.java / BidController.java
│   │   ├── service/              # Tầng xử lý logic nghiệp vụ lõi (Business Logic)
│   │   │   ├── AntiSnipingService.java      # Ngăn chặn hành vi phá giá giây cuối
│   │   │   ├── AuctionScheduler.java        # Tự động đóng/mở phiên theo thời gian thực
│   │   │   ├── AuctionService.java
│   │   │   ├── PaymentDeadlineChecker.java  # Quét và xử phạt quá hạn thanh toán
│   │   │   └── UserService.java / BidService.java / DepositService.java
│   │   ├── ClientHandler.java               # Luồng xử lý riêng biệt cho mỗi Client kết nối
│   │   └── MainServer.java                  # Điểm khởi chạy Server Socket chính
│   └── src/main/resources/
│       ├── data.sql / schema.sql            # Kịch bản cấu trúc SQL chuẩn bị cho tích hợp DB
│       └── database.properties              # Cấu hình tài khoản cơ sở dữ liệu
│
└── pom.xml                                  # Tệp tin cấu hình Maven cha (Parent POM)
```
## Cách build và chạy hệ thống

* **Build toàn bộ project:**
  mvn clean install


* **Chạy Server:**
  cd auction-server
  mvn exec:java -Dexec.mainClass="com.auction.server.MainServer"


* **Chạy Client:**
  cd auction-client
  mvn exec:java -Dexec.mainClass="com.auction.client.AuctionClientApp"


* **Lưu ý chạy trên Linux / MacOS:**
  Nếu gặp lỗi JavaFX:
  export JAVA_HOME=/path/to/jdk
  mvn clean javafx:run


---

## 5. Thứ tự chạy hệ thống

### 1. Chạy Server (auction-server)
* Khởi động lớp `MainServer`
* Server sẽ mở socket và lắng nghe kết nối từ client
* Khởi tạo các service và sẵn sàng xử lý:
    * Đăng nhập / xác thực
    * Đấu giá (bid)
    * Quản lý phiên đấu giá
* Server phải chạy liên tục để xử lý toàn bộ logic hệ thống

### 2. Chạy Client (auction-client)
* Khởi động `AuctionClientApp`
* Giao diện JavaFX được mở cho người dùng
* Client thực hiện kết nối tới server thông qua socket
* Người dùng có thể đăng nhập, xem danh sách đấu giá và tham gia bid

### 3. Cơ chế hoạt động
* Client gửi request (JSON/message) đến server
* Server xử lý nghiệp vụ và trả kết quả về client
* Các thay đổi (giá bid, trạng thái phiên đấu giá) được cập nhật gần realtime giữa các client

---

## 6. Chức năng đã hoàn thành
* Đăng nhập / phân quyền user
* Đăng bán sản phẩm
* Danh sách đấu giá real-time
* Đặt giá (bidding)
* Cơ chế chống sniping
* Quản lý admin panel
* Thông báo real-time
* Quản lý trạng thái đấu giá (`SCHEDULED`, `ACTIVE`, `CLOSED`)

---

## 7. Báo cáo và Demo

### Báo cáo PDF
* **Link:** `<Đính kèm Google Drive>`

### Video Demo
* **Link:** `<Đính kèm YouTube hoặc Google Drive>`
