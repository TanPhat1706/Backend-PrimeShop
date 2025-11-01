---Thẻ test VNPay---
Ngân hàng: NCB
Số thẻ: 9704198526191432198
Tên chủ thẻ: NGUYEN VAN A
Ngày phát hành:	07/15
Mật khẩu OTP: 123456

<<<<<<< HEAD
---Thẻ test MOMO---
No	Tên	            Số thẻ	                Hạn ghi trên thẻ	   OTP	    Trường hợp test
1	NGUYEN VAN A	9704000000000018	    03/07	               OTP	    Thành công
2	NGUYEN VAN A	9704000000000026	    03/07	               OTP	    Thẻ bị khóa
3	NGUYEN VAN A	9704000000000034	    03/07	               OTP	    Nguồn tiền không đủ
4	NGUYEN VAN A	9704000000000042	    03/07	               OTP	    Hạn mức thẻ
=======
2️⃣ Gửi request bằng Postman

Tất cả API nằm dưới prefix /api/bnpl.

🚀 Danh sách API
1️⃣ Khởi tạo BNPL

POST /api/bnpl/init

Body:

{
  "orderId": 3
}


Response:

{
  "status": "PENDING",
  "consentUrl": "https://sandbox.fundiin.vn/checkout?order=1"
}

2️⃣ Xác nhận BNPL (Fundiin callback)

POST /api/bnpl/confirm

Body:

{
  "orderId": 3,
  "status": "APPROVED"
}


Response:

{
  "message": "BNPL agreement updated",
  "agreementStatus": "APPROVED"
}

3️⃣ Lấy danh sách giao dịch BNPL

GET /api/bnpl/orders

Trả về danh sách BNPLAgreement.

4️⃣ Xem chi tiết giao dịch

GET /api/bnpl/orders/{orderId}

Trả về thông tin chi tiết theo orderId.

5️⃣ Gửi nhắc trả góp

POST /api/bnpl/reminder/{installmentId}

Response:

{ "message": "Reminder sent for installment 5" }

6️⃣ Thêm người dùng vào blacklist

POST /api/bnpl/blacklist/{userId}

Body:

{
  "reason": "Overdue payment"
}


Response:

{ "message": "User blacklisted" }

7️⃣ Danh sách blacklist

GET /api/bnpl/blacklist

Trả về danh sách user bị blacklist.

8️⃣ Báo cáo tổng hợp

GET /api/bnpl/reports

Response:

{
  "totalAgreements": 15,
  "approved": 9
}

9️⃣ Xuất danh sách BNPL ra CSV

GET /api/bnpl/export
→ Trả về file bnpl_orders.csv để tải xuống.

🔟 Cập nhật cấu hình API Fundiin

PUT /api/bnpl/config

Body:

{
  "apiKey": "FUNDIIN_SANDBOX_KEY",
  "sandbox": "true",
  "maxLimit": "10000000"
}


Response:

{ "message": "Config updated" }
>>>>>>> sprint1/thoai
