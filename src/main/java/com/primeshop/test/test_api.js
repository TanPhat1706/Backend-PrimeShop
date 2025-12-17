import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. Cấu hình kịch bản test (Options)
// Đây là nơi bạn định nghĩa "sức chịu đựng" mà bạn muốn test
export const options = {
  // A. Kịch bản đơn giản: 10 users chạy liên tục trong 30s
  // vus: 10,
  // duration: '30s',

  // B. Kịch bản "Senior" (Stages): Tăng dần tải (Ramp-up) để xem server chết ở đâu
  stages: [
    { duration: '10s', target: 10 },  // Tăng từ 0 lên 10 users trong 10s (Warm up)
    { duration: '30s', target: 50 },  // Tăng lên 50 users và duy trì trong 30s
    { duration: '10s', target: 0 },   // Giảm dần về 0 (Cool down)
  ],

  // Thiết lập tiêu chuẩn (Thresholds) - Test sẽ Fail nếu không đạt
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% requests phải nhanh hơn 500ms
    http_req_failed: ['rate<0.01'],   // Tỉ lệ lỗi phải dưới 1%
  },
};

// 2. Logic của từng Virtual User (VU)
export default function () {
  // Định nghĩa URL Localhost
  const url = 'http://localhost:8080/api/orders';

  // Định nghĩa Payload (Body gửi đi)
  const payload = JSON.stringify({
    productId: 123,
    quantity: 2,
    note: "Test stress load"
  });

  // Định nghĩa Headers (Quan trọng với Spring Boot @RequestBody)
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // Thực hiện Request
  const res = http.post(url, payload, params);

  // 3. Kiểm tra kết quả (Assert)
  // Spring Boot thường trả về 200 hoặc 201 nếu thành công
  check(res, {
    'status is 200': (r) => r.status === 200,
    'transaction success': (r) => r.body.includes('success'), // Check nội dung trả về (tùy chọn)
  });

  // Nghỉ 1s giữa các lần gọi để giả lập hành vi người thật (Think time)
  // Nếu muốn test max công suất server (DDOS), hãy comment dòng này.
  sleep(1);
}