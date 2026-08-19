# 🗼 CobbleTower (ViTwo)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.16.0+-blue.svg)](https://fabricmc.net/)
[![Cobblemon](https://img.shields.io/badge/Cobblemon-1.7.0+-red.svg)](https://cobblemon.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**CobbleTower** là bản mod mở rộng gameplay leo tháp **Roguelike Đấu Trường 100 Tầng** được thiết kế riêng biệt cho modpack **COBBLEVERSE - Minecraft 1.21.1 Fabric**, hỗ trợ cả hai chế độ **Đấu Đơn (Solo 2-Slot)** và **Đấu Đôi (Duo Co-op)** dưới thể thức **Double Battle (Đấu Đôi)** với đồ họa và giao diện chuẩn Cobblemon UI.

---

## ✨ Tính Năng Nổi Bật (Key Features)

### 1. ⚔️ Thể Thức Thi Đấu: Double Battle (Đấu Đôi)
* **Đấu Đơn (Solo Mode):** 1 người chơi điều khiển **cả 2 vị trí trên sân đấu** (xuất trận 2 Pokemon cùng lúc từ đội hình 6 con) đối đầu với Boss NPC.
* **Đấu Đôi (Duo Co-op Mode):** 2 người chơi hợp tác (mỗi người 1 vị trí, sử dụng 3 Pokemon đầu). Khi 1 người chơi ngất sạch Pokemon, người còn lại sẽ tiếp quản cả 2 vị trí (**2-Slot Takeover**), người ngất chuyển sang góc nhìn Khán Giả (**Spectator Mode**).

### 2. 📊 Hệ Thống Giới Hạn Cấp Độ (Level Cap System)
Bảo toàn cấp độ và điểm kinh nghiệm thật của Pokemon người chơi (tạo động lực huấn luyện ở thế giới ngoài) kết hợp luật giới hạn cấp theo từng chặng tầng:
* **Tầng 1 – 25:** Giới hạn **Max Lv. 36** (Boss NPC: Lv. 32 – 36).
* **Tầng 26 – 50:** Giới hạn **Max Lv. 50** (Boss NPC: Lv. 46 – 50).
* **Tầng 51 – 75:** Giới hạn **Max Lv. 80** (Boss NPC: Lv. 75 – 80 + **1 Pokemon Shiny ✨**).
* **Tầng 76 – 100:** Giới hạn **Max Lv. 100** (Boss NPC: Lv. 95 – 100 + **1 Pokemon Shiny ✨**).
> *Lưu ý:* Hệ thống tự động kiểm tra trước khi vào tháp. Nếu đội hình có Pokemon vượt cấp sẽ yêu cầu điều chỉnh lại cho phù hợp.

### 3. 👑 Boss NPC Hoàn Hảo & Shiny Siêu Hiếm
* **Chỉ số:** Toàn bộ Pokemon của Boss NPC sở hữu **Full 31 IVs ở cả 6 chỉ số (6x31 Perfect IVs)** và bảng EV chuẩn tối ưu thi đấu.
* **Pokemon Shiny:** Từ tầng 51 đến 100, mỗi đội hình Boss luôn có **ít nhất 1 Pokemon Shiny** đặc trưng của Boss.

### 4. 🎮 Kiến Trúc Command-Free 100% (Không dùng lệnh chat)
* **Phím Tắt `[Y]`:** Mở **`CobbleTower Hub`** bất kỳ lúc nào để:
  * Chuyển đổi giữa chế độ **Đấu Đơn** và **Đấu Đôi**.
  * Chọn mốc Checkpoint ($1, 10, 25, 50, 75, 90$) — tự động áp dụng `min(CP_A, CP_B)` khi đấu đôi.
  * Phản hồi lời mời leo tháp từ bạn bè với 2 nút `[ĐỒNG Ý]` / `[TỪ CHỐI]` trực tiếp trong GUI.
  * Bấm `[BẮT ĐẦU LEO THÁP]` để tự động dịch chuyển ngầm vào đấu trường.
* **Cổng Thế Giới (Gateway Block):** Khối `vitwo:tower_gateway` đặt tại sảnh chờ, chuột phải để mở Hub.
* **Mời nhanh:** Nhìn vào bạn bè và bấm **`Shift + Chuột Phải`** để gửi lời mời leo tháp. Người nhận sẽ thấy **Toast Popup** ở góc phải màn hình.

### 5. ⛺ Tầng Nghỉ Roguelike (Rest Floor - Mỗi 5 Tầng)
Tại các tầng $5, 10, 15, 20...$, người chơi được lựa chọn 1 trong 2 quyền lợi:
* **🟢 Hồi Phục Đội Hình:**
  * Pokemon đã ngất: Hồi sinh với **10% Max HP**.
  * Pokemon còn sống: Hồi phục thêm **+50% Max HP** (tối đa 100%).
  * Toàn bộ đội hình: Hồi **100% PP** cho mọi chiêu thức và xóa bỏ mọi hiệu ứng trạng thái bất lợi.
* **🎁 Rương Quà Quý & Buff:** Không hồi máu, nhận thêm vật phẩm giá trị cao (EXP Candy XL, Bottle Caps, CobbleDollars...).

### 6. 🔥 4 Battle Gimmicks & Luật Cấm Túi Đồ
* **Gimmicks:** Người chơi được tự do sử dụng mọi Gimmick mình có. Boss NPC kích hoạt theo chặng:
  * **Tầng 1 – 25:** Không Gimmick
  * **Tầng 26 – 50:** Terastallization (Tera) & Z-Moves
  * **Tầng 51 – 75:** Mega Evolution & Dynamax
  * **Tầng 76 – 100:** Full Gimmicks (Kết hợp cả 4 cơ chế)
* **Vật phẩm trang bị (Held Items):** Được phép giữ nguyên và sử dụng thoải mái mọi Held Items.
* **Cấm Túi Đồ trong trận:** Cấm sử dụng Potion, Revive từ túi đồ trong khi đang đấu.

### 7. ⏱️ Chống Rớt Mạng & An Toàn Phiên Đấu
* Khi một người chơi mất kết nối giữa trận, phiên đấu sẽ tự động tạm dừng và đếm ngược **3 phút (180 giây)** chờ người chơi kết nối lại.

---

## ⌨️ Phím Tắt & Điều Khiển Mặc Định

| Phím Tắt | Hành Động | Ghi Chú |
| :--- | :--- | :--- |
| **`Y`** | Mở CobbleTower Hub | Xem tiến trình, chọn Checkpoint, nhận lời mời, xuất trận |
| **`Shift + Chuột Phải`** | Mời bạn bè leo tháp | Nhìn trực tiếp vào người chơi khác |
| **Chuột Phải vào Block** | Tương tác Cổng `vitwo:tower_gateway` | Mở CobbleTower Hub |

---

## 🛠️ Hướng Dẫn Cài Đặt & Sử Dụng

### Yêu Cầu Cài Đặt:
* **Minecraft:** `1.21.1`
* **Fabric Loader:** `>= 0.16.0`
* **Fabric API:** Bản tương ứng cho 1.21.1
* **Cobblemon:** `>= 1.7.0` (Hoặc modpack COBBLEVERSE)

### Cách Cài Đặt File `.jar`:
1. Tải file `CobbleTower-1.0.0.jar` từ mục **Releases** hoặc biên dịch từ mã nguồn.
2. Sao chép file `.jar` vào thư mục `mods` của Minecraft / PrismLauncher / CurseForge.
3. Khởi động game và bấm phím **`[Y]`** hoặc đặt khối **Cổng Leo Tháp** để bắt đầu trải nghiệm!

---

## 🔨 Biên Dịch Từ Mã Nguồn (Build from Source)

```bash
# Clone repository
git clone https://github.com/namtacozz/ViTwo.git
cd ViTwo

# Build file mod JAR
./gradlew build
# hoặc
gradle build
```
File mod hoàn chỉnh sau khi build sẽ nằm tại: `build/libs/vitwo-1.0.0.jar`

---

## 📜 Giấy Phép (License)
Dự án được phát hành theo giấy phép [MIT License](LICENSE).
