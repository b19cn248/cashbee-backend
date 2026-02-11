# Plan: Handle All Shopee Link Formats

## Mục tiêu
Cập nhật hàm `createTrackingLink` để xử lý TẤT CẢ các định dạng link Shopee có thể xảy ra khi user copy từ app/web.

## Phân tích hiện trạng

### Các định dạng link Shopee đã biết (từ nghiên cứu)

| # | Domain | Ví dụ | Mô tả |
|---|--------|-------|-------|
| 1 | `shopee.vn` | `https://shopee.vn/product/123/456` | Link đầy đủ - đã hỗ trợ |
| 2 | `shopee.vn` | `https://shopee.vn/Product-Name-i.123.456` | Link có tên SP - đã hỗ trợ |
| 3 | `shopee.vn` | `https://shopee.vn/shop/123/456` | Link từ shop - đã hỗ trợ |
| 4 | `s.shopee.vn` | `https://s.shopee.vn/5Al0npFYE8` | Link rút gọn (subdomain) - đã hỗ trợ |
| 5 | `vn.shp.ee` | `https://vn.shp.ee/S5hDghe` | Link rút gọn (domain riêng) - **CHƯA HỖ TRỢ** |
| 6 | `shope.ee` | `https://shope.ee/3poh74Bvt2` | Link affiliate global - **CHƯA HỖ TRỢ** |

### Code hiện tại

**File: `ShopeeUrlExpanderService.java`**
```java
public boolean isShortenedUrl(String url) {
    // CHỈ MATCH: s.shopee.vn, s.shopee.sg, ...
    return url.matches("https?://s\\.shopee\\.[a-z]{2,3}/.*");
}
```

**Vấn đề:** Không nhận diện được:
- `vn.shp.ee` (domain `shp.ee` với prefix quốc gia)
- `shope.ee` (domain affiliate global)

## Plan thực hiện

### Bước 1: Cập nhật `ShopeeUrlExpanderService.isShortenedUrl()`
**Mục đích:** Nhận diện TẤT CẢ các định dạng link rút gọn của Shopee

**Thay đổi:**
```java
public boolean isShortenedUrl(String url) {
    if (url == null || url.isBlank()) {
        return false;
    }

    // Pattern 1: s.shopee.{country} (e.g., s.shopee.vn, s.shopee.sg)
    // Pattern 2: {country}.shp.ee (e.g., vn.shp.ee, id.shp.ee, ph.shp.ee)
    // Pattern 3: shope.ee (global affiliate short link)
    return url.matches("https?://s\\.shopee\\.[a-z]{2,3}/.*")      // s.shopee.vn
        || url.matches("https?://[a-z]{2}\\.shp\\.ee/.*")          // vn.shp.ee
        || url.matches("https?://shope\\.ee/.*");                   // shope.ee
}
```

### Bước 2: Kiểm tra và cập nhật `expandUrl()` nếu cần
**Mục đích:** Đảm bảo expand URL hoạt động với tất cả domain

**Logic hiện tại:** Dùng HTTP redirect để expand → Đã OK, không cần thay đổi
- Tất cả các domain đều redirect về `shopee.vn`
- Method `expandUrl()` follow redirect chain → sẽ hoạt động tự động

### Bước 3: Thêm logging chi tiết
**Mục đích:** Debug dễ dàng hơn khi có format mới

**Thay đổi:** Log loại shortened URL được detect

### Bước 4: Viết unit tests (nếu cần)
**Mục đích:** Đảm bảo tất cả formats được xử lý đúng

## Chi tiết thay đổi

### File cần sửa
1. `ShopeeUrlExpanderService.java` - Cập nhật method `isShortenedUrl()`

### File KHÔNG cần sửa
- `ShopeeUrlParser.java` - Đã handle đúng sau khi URL được expand
- `CreateTrackingLinkUseCase.java` - Đã gọi đúng flow
- `AffiliateTrackingController.java` - Không cần thay đổi

## Tóm tắt

| Công việc | File | Độ phức tạp |
|-----------|------|-------------|
| Cập nhật regex nhận diện shortened URL | `ShopeeUrlExpanderService.java` | Thấp |
| Thêm logging | `ShopeeUrlExpanderService.java` | Thấp |

**Ước tính:** ~10 dòng code thay đổi

## Rủi ro
- **Thấp:** Các domain mới (`vn.shp.ee`, `shope.ee`) đều redirect về `shopee.vn` nên logic expand không đổi
- **Cần theo dõi:** Shopee có thể thêm domain mới trong tương lai

## Approval Required
- [ ] User review và approve plan này trước khi implement
