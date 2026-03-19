🛒 PrimeShop - Fullstack E-commerce Platform
📝 Giới thiệu dự án
PrimeShop là dự án thương mại điện tử hoàn chỉnh được thực hiện trong kỳ thực tập/đồ án năm 4. Hệ thống cho phép người dùng trải nghiệm quy trình mua sắm khép kín từ tìm kiếm sản phẩm, quản lý giỏ hàng đến thanh toán trực tuyến qua các cổng VNPay và MoMo.Dự án được vận hành theo mô hình Agile/Scrum với mục tiêu tối ưu hóa trải nghiệm người dùng và đảm bảo tính bảo mật cho dữ liệu giao dịch.
👨‍ Quản trị & Điều phối (Project Management)
Mô hình: Agile/Scrum (Sprint 2 tuần).
Vai trò: Project Manager & Technical Lead.
Trách nhiệm chính: * Thiết kế kiến trúc hệ thống và chuẩn hóa cấu trúc thư mục (Folder Structure).
Điều phối công việc cho đội ngũ, quản lý tiến độ và review mã nguồn.
Tích hợp các cổng thanh toán điện tử (VNPay/MoMo/PayPal).
🚀 Tính năng nổi bật
[x] Xác thực & Phân quyền: Sử dụng Spring Security & JWT (Role-based access: Admin/User/Seller).
[x] Quản lý sản phẩm: Tìm kiếm, lọc theo danh mục, quản lý kho hàng.
[x] Giỏ hàng & Đơn hàng: Luồng đặt hàng tối ưu, quản lý trạng thái đơn hàng.
[x] Thanh toán trực tuyến: Tích hợp thành công cổng thanh toán VNPay, MoMo và PayPal.
[x] Voucher & Minigame : Tham gia trò chơi nhỏ của hệ thống và nhận về các mã khuyến mãi áp dụng để giảm trực tiếp đơn hàng hoặc miễn phí vận chuyển.
[x] Báo cáo & Thống kê: Dashboard dành cho Admin theo dõi doanh thu và số lượng đơn hàng.
💻 Công nghệ sử dụng
Backend
Core: Java 17, Spring Boot 3.x
Security: Spring Security, JWT (JSON Web Token)
Persistence: Spring Data JPA, Hibernate
Database: Microsoft SQL Server
Build Tool: Maven
🛠 Hướng dẫn cài đặt (Setup)
1. Cấu hình DatabaseSử dụng SQL Server Management Studio (SSMS).
Chuột phải Databases -> Restore Database...Chọn file primeshop.bak từ thư mục dự án và nhấn OK.
Cấu hình thông tin kết nối tại: src/main/resources/application.properties.
2. Chạy Backend
mvn clean install
mvn spring-boot:run
💳 Thông tin kiểm thử thanh toán (Test Cards)
Cổng VNPay
Ngân hàng       Số thẻ                 Tên chủ thẻ       Ngày phát hành        OTP
NCB          9704198526191432198      NGUYEN VAN A         07/15123456       123456
Cổng MoMo (Ví dụ trường hợp thành công)
Tên chủ thẻ        Số thẻ                 Hạn thẻ     OTP      Kết quả
NGUYEN VAN       A9704000000000018        03/07      123456   Thành công
📁 Cấu trúc thư mục tiêu biểu
Dự án được thiết kế theo cấu trúc phân tầng (Layered Architecture) giúp dễ dàng mở rộng và bảo trì:
controller/: Tiếp nhận và xử lý các Request.
service/: Chứa các Business Logic chính của hệ thống.
repository/: Tương tác trực tiếp với cơ sở dữ liệu qua JPA.
security/: Cấu hình JWT và bộ lọc an ninh.
dto/: Các đối tượng chuyển đổi dữ liệu để tối ưu hóa API.
