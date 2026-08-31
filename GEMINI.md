# 🚀 ViTwo (CobbleTower) - Antigravity Agent Master Guidelines

> **Tệp cấu hình hệ thống (System Instructions) bắt buộc đọc trong mọi session của Antigravity Agent khi làm việc trong Workspace này.**
> Tệp này đóng vai trò là "Sách Khải Huyền" lưu trữ toàn bộ kiến trúc lõi (Core Architecture), logic quy trình xử lý, các phân hệ Gacha, và các nguyên tắc code bất di bất dịch của dự án **CobbleTower v1.8.0** (tích hợp Cobblemon + RCTMod trên Fabric 1.21.1).

---

## 🏗️ 1. KIẾN TRÚC HỆ THỐNG LÕI (CORE ARCHITECTURE)

Hệ thống Mod được phân chia thành các Manager độc lập đóng vai trò trụ cột. **Tuyệt đối không phá vỡ liên kết giữa các Manager này**.

### ⚔️ `TowerBattleManager.java` & `RCTModAdapter.java` (Xử Lý Trận Đánh & Tương Tác RCTMod)

- **Nhiệm vụ:** Lắng nghe và can thiệp trực tiếp vào API của Cobblemon (`CobblemonEvents.BATTLE_STARTED_POST`, `BATTLE_VICTORY`, `BATTLE_FLED`).
- **RCTMod Native Invocation:** Sử dụng `RCTModAdapter.java` chứa các `MethodHandle` đã cache tĩnh để gọi trực tiếp các phương thức RCTMod (`makeBattle`, `setOpponent`, `addBattle`), triệt tiêu hoàn toàn runtime reflection overhead.
- **Quy tắc Level & Đội hình:**
  - **Trong Tháp (`vitwo:tower_dimension`):** Đội hình NPC Trainer *bắt buộc* được ghi đè tại sự kiện `BATTLE_STARTED_POST` hoặc trong `BattleRegistryMixin.startBattle`. Phải nạp chính xác đội hình 6 Pokémon dạng Competitive (Chuẩn EVs, 6x31 IVs, Movesets, Items, Nature) được định nghĩa trong `tower_teams.json` (thông qua `HellModeTeamLoader` / `TrainerPool`) và ép cấp theo `LevelCapManager.getMaxLevelCapForFloor(floor)`.
  - **Ngoài Thế Giới Thực (Overworld Hell Mode 2v2 Double):** Tất cả các trận đấu với NPC Trainer (Gym Leader, Elite Four, Champion, RCTMod Trainers) ngoài thế giới thực đều được tự động chuyển thành **2v2 Double Battle (`GEN_9_DOUBLES`)** và nạp đội hình **Hell Mode** chuẩn Competitive từ `HellModeTeamLoader` (bảo toàn cấp độ gốc của Trainer ngoài Overworld) để mang lại trải nghiệm phiêu lưu thế giới mở kịch tính và hardcore bậc nhất.
- **Quy tắc Kiểm tra Không gian (Dimension Check & Isolation):** TẤT CẢ các logic đặc thù của Tháp (như Downscale cấp độ `applyLevelCapToPlayer`, giải tán phiên chơi leo tháp `disbandParty` và dịch chuyển về Hub khi thua, tính điểm BP, rút Gacha Boss, chuyển tầng `onFloorWon`, Spectator Ghost, và đếm giờ leo tháp) BẮT BUỘC phải kiểm tra người chơi có đang ở trong `vitwo:tower_dimension` hay không. Việc này ngăn chặn triệt để lỗi "Overworld Party Leak" (Logic của Tháp can thiệp nhầm vào trận đấu Gym ngoài thế giới thực gây ngắt quãng hoặc dịch chuyển người chơi về Hub).

### 🏛️ `TowerArenaManager.java` (Quản Lý Không Gian & Sinh Thành)

- **Nhiệm vụ:** Xử lý việc tạo sàn đấu trong Dimension riêng biệt (`vitwo:tower_dimension`).
- **Cấp phát Sàn đấu $O(1)$:** Sử dụng `java.util.BitSet` (`occupiedSlots.nextClearBit(0)`) để cấp phát và giải phóng slot khu vực đấu cực nhanh, chống phân mảnh.
- **Quy tắc Sinh Thành (Spawning):**
  - Vị trí khu vực của một nhóm người chơi (Sector) phải thay đổi liên tục nhờ thuật toán `party.getAttemptId() % 70` để tránh trùng lặp công trình cũ.
  - Khi đặt công trình NBT (Modpack Structures), bắt buộc phải có bước xóa các khối kỹ thuật như `Blocks.JIGSAW`, `Blocks.STRUCTURE_BLOCK`, `Blocks.STRUCTURE_VOID`.
  - Phải luôn dò tìm vị trí mặt sàn an toàn bằng thuật toán `findTopWalkableY()`. Với tầng 100 (`temple_of_sinnoh`), trần nhà tối đa là Y=78 (Bệ thờ Spear Pillar), cấm spawn ở nóc nhà Y=95.

### 👥 `TowerPartyManager.java` (Quản Lý Phiên Chơi & Đội)

- **Nhiệm vụ:** Xử lý logic vào tháp, đầu hàng, tiến lên tầng tiếp theo của Solo (6v6 Double Battle) và Co-op Duo (3+3 vs 3+3 Multi-Battle `GEN_9_MULTI`).
- **Quy tắc Vòng đời (Lifecycle):** Bất cứ khi nào bắt đầu hoặc kết thúc tháp (Forfeit/Thắng/Thua), hệ thống phải dọn dẹp các Entity, xóa cấu trúc cũ trong Dimension, cập nhật Checkpoint, giải phóng slot arena, và cấp một `attemptId` mới tinh cho Party.
- **Hệ thống Chống Treo (Anti-Stall):** Phương thức `tick()` duy trì một bộ đếm `hardStallTicks`. Phải luôn thiết lập bộ đếm này đủ dài (tối thiểu 600 ticks - 30 giây) để phòng ngừa lỗi kích hoạt nhầm khi người chơi đấu đôi (Double Battle) và hoạt ảnh tung Pokéball diễn ra quá lâu.
- **Dọn dẹp Lời mời Duo (TTL Cleanup):** Lời mời tổ đội Duo tự động hết hạn sau 60 giây và được giải phóng bộ nhớ định kỳ.

### 💾 `TowerRunPersistenceManager.java` & `TowerPersistenceService.java` (Lưu Trữ & Khôi Phục Bất Đồng Bộ)

- **Asynchronous Persistence Pipeline:** Toàn bộ thao tác ghi đĩa cho Profile người chơi (`saveProfile`) và dữ liệu Run leo tháp (`saveToFile`) được thực hiện bất đồng bộ qua `TowerPersistenceService` trên luồng riêng, triệt tiêu hoàn toàn hiện tượng tụt TPS/khựng máy chủ.
- **Quy tắc Serialization (Gson):** Tuyệt đối KHÔNG sử dụng `Map<UUID, Map<UUID, Integer>>` để lưu trữ dữ liệu đa tầng với Gson vì tính chất Type Erasure của Java. Bắt buộc phải ép kiểu Key bên trong thành `String` (Ví dụ: `Map<UUID, Map<String, Integer>> originalPokemonLevels`).
- **Failsafe Khôi Phục Cấp Độ Đa Tầng:** Bổ sung `LevelCapManager.restorePlayerLevelsFromRunData` tự động khôi phục 100% cấp độ gốc và EXP nhóm (Slow, Fluctuating...) cho người chơi nếu máy chủ bị crash hoặc tắt đột ngột khi người chơi đang trong tháp.

---

## 🎰 2. PHÂN HỆ GACHA VÒNG QUAY CS:GO (CS:GO GACHA SYSTEM)

CobbleTower v1.8.0 tích hợp hệ thống Gacha vòng quay phong cách CS:GO hoàn chỉnh với 2 phân hệ độc lập:

### 🏆 2.1. Vòng Quay Pokémon Tầng Boss Đa Tầng (`TowerPokemonGachaScreen.java`)

- **Kích hoạt:** Tại các mốc tầng Boss (Gym Leader %5, Tứ Hoàng 90-99, Cynthia 100).
- **Tập hợp Pool Pokémon:** Tự động gom toàn bộ Pokémon của đối thủ đã gặp từ các tầng trước đó kể từ lần quay gần nhất (vd: Tầng 5 lấy từ 1-5; Tầng 10 lấy từ 6-10...) thông qua `TowerParty.recordEncounteredPokemon()` và `party.drainEncounteredCandidates(floor)`.
- **Phân Cấp Độ Hiếm CS:GO (`PokemonRarity.java`):**
  - 👑 **High Legend (Vàng Kim/Đỏ - `0xFFFF3344` / `0xFFFFD700`):** Box/Cover Legendaries (Mewtwo, Rayquaza, Arceus, Koraidon, Miraidon...).
  - 💜 **Low Legend (Tím - `0xFF9B59B6`):** Sub-Legendaries (Tam Điểu, Tam Khuyển, Latios, Urshifu, Paradox...).
  - 💖 **Mythical (Hồng/Magenta - `0xFFFF1493`):** Mew, Celebi, Jirachi, Deoxys, Darkrai, Pecharunt...
  - 💎 **Á Thần / Pseudo-Legendary (Cyan-Tím - `0xFF8A2BE2`):** Dragonite, Tyranitar, Salamence, Garchomp, Dragapult...
  - 🔷 **Starter (Xanh lam - `0xFF1E90FF`):** Dòng khởi đầu Gen 1-9.
  - ⚪ **Common (Xám/Xanh lá - `0xFF7F8C8D`):** Các loài Pokémon thông thường khác.
- **Chuỗi 3 Vòng Quay Liên Hoàn:**
  - **Stage 1 (Vòng quay ngang Loài):** Băng chuyền 50-60 thẻ trượt ngang với hàm giảm tốc `Ease-Out Quart`, vạch đỏ chỉ tâm và âm thanh click tick qua từng thẻ.
  - **Stage 2 (Vòng quay Shiny 1%):** Vòng quay phụ thử vận may 1% trúng Pokémon Shiny lấp lánh (với hiệu ứng ánh kim cầu vồng).
  - **Stage 3 (Bảng 6 Trục Dọc IVs):** 6 trục quay dọc cho 6 dòng chỉ số (HP, Atk, Def, SpA, SpD, Spe). Hỗ trợ quay từng dòng riêng lẻ hoặc bấm **"🎲 QUAY TẤT CẢ (SPIN ALL)"**. Trúng 31 Max IV sẽ phát sáng vàng kim rực rỡ và kích hoạt hiệu ứng Level-Up.
  - **Stage 4 (Tổng kết & Trao Thưởng):** Nhận Pokémon sơ cấp (Base Form Lv.1) với đúng Shiny, IVs và Regional Form chuyển thẳng vào Party (hoặc PC Box nếu đầy 6/6). Nếu quay trúng Huyền Thoại/Thần Thoại không thể sinh trứng, tự động tặng 1x Master Ball + 500 BP thay thế.

### 🎁 2.2. Vòng Quay Vật Phẩm & BP Tầng Thường (`TowerItemGachaScreen.java`)

- **Kích hoạt:** Sau mỗi tầng đánh thắng thông thường (Tầng 1-4, 6-9, 11-14...).
- **Phân cấp phẩm chất (4 Tiers):**
  - **Tier 3 (Jackpot / Gold):** Master Ball, Gold Bottle Cap, Ability Patch, 1,500 BP Jackpot.
  - **Tier 2 (Rare / Purple):** Bottle Cap, Ability Capsule, Choice Items, Life Orb, Focus Sash, 500 BP.
  - **Tier 1 (Uncommon / Blue):** EV Vitamins (HP Up, Carbos...), Evolution Stones, Mints, EXP Candy XL, 250 BP.
  - **Tier 0 (Common / Gray):** Poké Balls, Max Elixirs, Revival Herbs, Rare Candies, 100 BP.
- **Tối ưu tốc độ:** Tích hợp nút **"SPIN"** và **"BỎ QUA / NHẬN NGAY (SKIP)"** để không làm chậm nhịp độ leo tháp tốc độ cao.

---

## 🖥️ 3. QUY CHUẨN GIAO DIỆN & HUD (UI/UX RULES)

- **100% English In-Game UI/UX (Bắt buộc tiếng Anh toàn phần):** Toàn bộ giao diện (UI), HUD, tiêu đề, nút bấm (Buttons), thông báo Chat, Toast, lời thoại và thông điệp server trong game BẮT BUỘC sử dụng 100% Tiếng Anh (English Only). Tuyệt đối KHÔNG sử dụng tiếng Việt hay song ngữ trong in-game client/server strings.
- Toàn bộ các tương tác nút bấm trong màn hình Custom (như `TowerHubScreen`, `TowerBpShopScreen`, `TowerPokemonGachaScreen`, `TowerItemGachaScreen`) phải được thiết kế dạng **Modern Flat Dark Slate Cyber Buttons** (`TowerButton.java`).
- Mã màu văn bản khi vẽ bằng `context.drawTextWithShadow` hoặc `context.drawCenteredTextWithShadow` **bắt buộc phải có kênh Alpha đầy đủ `0xFF000000`** (Ví dụ: `0xFFFFD700`, `0xFFFFFFFF`, `0xFFCCCCCC`) để tránh hiện tượng chữ bị mờ hoặc tàng hình trên các phiên bản Fabric mới.
- **HUD Trực Tiếp (`TowerHudOverlay.java`):**
  - **Top Progress Box:** Hiển thị phân cấp màu tầng (Gold Cynthia 100, Magenta Elite 4 90-99, Orange Gym 5, Cyan Thường), Cap cấp độ động (`Cap: Lv.X`), tên Boss, tự động co giãn chiều cao (38px / 50px khi có Curse sàn đấu).
  - **Bottom-Left Pill Box:** Hiển thị thời gian leo tháp thời gian thực (`⏱ MM:SS`), lượt đấu (`Turn X`), và tổng BP kiếm được trong phiên (`+X BP`).

---

## 🛠️ 4. NGUYÊN TẮC LẬP TRÌNH BẮT BUỘC (CODING GUIDELINES)

1. **Khởi tạo Pokémon tùy chỉnh:** KHÔNG can thiệp trực tiếp bằng reflection nếu không cần thiết. Luôn ưu tiên sử dụng `PokemonProperties.Companion.parse(...).create()` kết hợp chuỗi tham số để khởi tạo chuẩn xác (VD: `species level=100 ivs=31,31,31,31,31,31 shiny=true moves=x,y,z`).
2. **Khởi tạo NPC (TrainerMob - RCTMod):** Luôn khởi tạo Trainer NPC của mod RCTMod (`rctmod:trainer`) thông qua `EntityType`, set tọa độ bằng `setHomePos`, set Trainer ID (`TrainerPool.getRctTrainerIdForFloor()`), và đảm bảo bật cờ `setPersistent(true)`.
3. **Môi trường Build (Build Environment):** Dự án sử dụng Java 21 và Fabric 1.21.1. Khi chạy lệnh build, bắt buộc phải trỏ biến môi trường `JAVA_HOME` đến Java 21:
   ```bash
   JAVA_HOME="/home/arjunsharma/.local/share/PrismLauncher/java/java-runtime-delta" ./gradlew build
   ```
   **Sau khi sửa lỗi hoặc nâng cấp tính năng xong, BẮT BUỘC phải chạy lệnh build lại mod và đảm bảo file JAR mới đã được copy tự động (via Gradle Deploy) vào thư mục mods của PrismLauncher để người chơi có thể test ngay lập tức.**
4. **Không Warning/Lỗi thời (No Deprecation):** Khi tương tác với BlockStates của MC 1.21, phải dùng `state.getFluidState().isEmpty()` thay vì `state.isLiquid()` để tránh cảnh báo.
5. **Hồi phục Full PP/HP Tuyệt Đối:** Sử dụng `TowerRewardManager.fullHeal(mon)` và `PartyStore.sendTo(player)` để đảm bảo 100% PP thật trên cả Showdown battle actor và Minecraft client inventory sau mỗi trận đấu.

---

## 📈 5. LỊCH SỬ CẬP NHẬT KIẾN TRÚC (CHANGELOGS)

- **v1.4.1 - v1.5.8:** Hoàn thiện cơ chế hồi phục 100% HP/PP, cân bằng cấp độ động, Duo Co-op Multi-Battle (`GEN_9_MULTI`), bảo vệ Forfeit Duo, sửa dứt điểm lỗi tụt cấp EXP nhóm (Lv.97 -> 100) và lỗi NPC đơ/văng trận.
- **v1.6.0:** Tính toán chính xác ngưỡng EXP nhóm (`mon.getExperienceGroup().getExperience(targetLevel)`), đồng bộ thời gian thực BP Shop & Hub Profile qua `RequestHubSyncC2SPacket`.
- **v1.7.0 (Tái Cấu Trúc Toàn Diện & Tối Ưu Hóa Hiệu Năng):**
  - Tách biệt luồng lưu đĩa bất đồng bộ qua `TowerPersistenceService.java`.
  - Giảm thiểu rò rỉ bộ nhớ với Bounded LRU Cache (64 entries) cho `RUN_SCHEDULES` và cơ chế TTL 60s cho lời mời Duo.
  - Xây dựng `RCTModAdapter.java` với cached MethodHandles triệt tiêu reflection trong hot paths.
  - Cấp phát sàn đấu $O(1)$ với `BitSet.nextClearBit(0)`.
  - Cơ chế khôi phục cấp độ Failsafe đa tầng khi server crash.
- **v1.8.0 (CS:GO Case Opening Gacha System - Vòng Quay Đa Tầng Hoàn Chỉnh):**
  - **Vòng Quay Pokémon Tầng Boss Đa Tầng (`TowerPokemonGachaScreen.java`):**
    - Tập hợp Pool Pokémon đã gặp từ các tầng trước (`drainEncounteredCandidates`).
    - Phân cấp độ hiếm và gắn thanh màu CS:GO (`PokemonRarity.java`): High Legend (Vàng/Đỏ), Low Legend (Tím), Mythical (Hồng), Á Thần (Cyan-Tím), Starter (Xanh lam), Common (Xám).
    - Vòng quay ngang CS:GO 50-60 thẻ với hàm giảm tốc `Ease-Out Quart`, vạch đỏ tâm và ticker sound.
    - Vòng quay phụ Shiny 1% (Stage 2) với hiệu ứng ánh kim cầu vồng.
    - Bảng 6 trục quay dọc IVs (Stage 3) hỗ trợ quay từng dòng hoặc "Spin All" với hiệu ứng phát sáng vàng kim khi trúng 31 Best IV.
    - Trao thưởng Pokémon sơ cấp Lv.1 kế thừa IVs, Shiny và Regional Form vào Party/PC Box.
  - **Vòng Quay Vật Phẩm & BP Tầng Thường (`TowerItemGachaScreen.java`):**
    - Vòng quay CS:GO ngẫu nhiên các phần thưởng theo 4 bậc phẩm chất (Jackpot, Rare, Uncommon, Common) kèm nút "Spin" và "Skip".

> 🤖 **Lời nhắc dành cho AI Agent:** Bất kể yêu cầu của người dùng là gì, hãy luôn kiểm tra chéo với tệp `GEMINI.md` này để đảm bảo giải pháp của bạn không vô tình phá vỡ kiến trúc lõi của CobbleTower. Cập nhật thêm thông tin vào file này nếu bạn triển khai một hệ thống Manager/Logic phức tạp mới!
