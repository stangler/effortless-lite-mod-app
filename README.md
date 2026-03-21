# Effortless Lite

Minecraft 1.21.1 / NeoForge 向けの簡易建築支援Mod。
[Effortless Building](https://www.curseforge.com/minecraft/mc-mods/effortless-building) にインスパイアされた軽量版です。

---

## インストール方法

### 必要なもの
- Minecraft Java Edition 1.21.1
- NeoForge 21.1.172

### 手順

1. [NeoForge 公式サイト](https://neoforged.net/) から **NeoForge 21.1.172** のインストーラーをダウンロードして実行する
2. `effortlesslite-1.0.0.jar` を以下のフォルダに置く
   ```
   %appdata%\.minecraft\mods\
   ```
3. Minecraft Launcher で **NeoForge 1.21.1** プロファイルを選択して起動する

---

## 機能一覧

| 機能 | 説明 |
|------|------|
| **Line** | 始点と終点を結ぶ直線にブロックを配置 |
| **Wall** | 始点と終点の間に垂直な壁を配置 |
| **Floor** | 始点と終点で囲まれた水平な床を配置 |
| **Cube** | 始点と終点のバウンディングボックス全体を埋める |
| **ミラー** | プレイヤー位置を中心に X軸 / Z軸 / XZ軸 対称配置 |
| **Undo/Redo** | 操作の取り消し・やり直し（最大20操作） |
| **拡張リーチ** | 最大32ブロック先にブロックを配置可能 |
| **プレビュー** | 配置予定ブロックをシアン色のアウトラインで可視化 |

---

## キーバインド

| キー | 操作 |
|------|------|
| `G` | ビルドモードを切り替え (Normal → Line → Wall → Floor → Cube → ...) |
| `H` | ミラー軸を切り替え (None → X → Z → XZ → ...) |
| `Z` | Undo（直前の配置を取り消す） |
| `Y` | Redo（取り消した配置をやり直す） |
| `Escape` | 始点をリセットしてキャンセル |

> キーバインドはゲームの「設定 > コントロール > Effortless Lite」から変更できます。

---

## 使い方

1. **[G]** キーでビルドモードを選ぶ（例: Wall）
2. 手にブロックを持つ
3. 壁の**始点**を右クリック → 画面にシアン色のアウトラインが表示される
4. 壁の**終点**を右クリック → ブロックが一括配置される
5. ミラーを使う場合は **[H]** で軸を設定してから上記を行う

> **Normal モード**の場合は1クリックで即座に1ブロック配置されます。

---

## プロジェクト構成

```
src/main/java/com/example/effortlesslite/
├── EffortlessLite.java          # メインクラス
│
├── build/
│   ├── BuildMode.java           # ビルドモード列挙 (NORMAL/LINE/WALL/FLOOR/CUBE)
│   ├── MirrorAxis.java          # ミラー軸列挙 (NONE/X/Z/XZ)
│   ├── BuildState.java          # クライアント側状態管理 (始点・モード)
│   └── BlockCalculator.java     # ブロック座標計算ロジック (コアアルゴリズム)
│
├── client/
│   ├── ModKeyBindings.java      # キーバインド定義・登録
│   ├── ClientBuildHandler.java  # キー入力・右クリックの処理
│   ├── PreviewRenderer.java     # ゴーストブロックのアウトライン描画
│   └── HudRenderer.java         # 画面左上のHUD表示
│
├── network/
│   ├── ModNetwork.java          # パケット登録
│   ├── PlaceBlocksPacket.java   # C→S: ブロック配置リスト送信
│   ├── UndoPacket.java          # C→S: Undo要求
│   ├── RedoPacket.java          # C→S: Redo要求
│   └── SyncModePacket.java      # C→S: ビルドモード同期 (拡張リーチ用)
│
└── server/
    ├── PlayerBuildData.java     # プレイヤーごとのデータ (Undo/Redoスタック)
    ├── ServerBuildHandler.java  # 実際のブロック配置・Undo/Redo処理
    └── ServerEventHandler.java  # ログイン/ログアウト時の後処理
```

---

## ビルド方法

### 必要なもの
- JDK 21
- インターネット接続 (Gradle が NeoForge をダウンロード)

### コマンド

```bash
# Windows
gradlew.bat build

# Mac / Linux
./gradlew build
```

ビルド成功後、`build/libs/effortlesslite-1.0.0.jar` が生成されます。
これを `.minecraft/mods/` フォルダに置いてください。

### 開発時の起動

```bash
# クライアント起動 (ゲームが立ち上がる)
./gradlew runClient

# サーバー起動
./gradlew runServer
```

---

## 制限事項

| 項目 | 制限値 |
|------|--------|
| 一度に配置できるブロック数 | 最大 512 ブロック |
| Undo/Redo 履歴数 | 最大 20 操作 |
| 拡張リーチ距離 | 最大 32 ブロック |

---

## 技術仕様

- **Minecraft**: 1.21.1
- **ModLoader**: NeoForge 21.1.172
- **Java**: 21
- **ブロック座標計算**: `BlockCalculator.java` でモードごとにアルゴリズムを実装
- **ネットワーク**: C→S のみの単方向通信 (クライアントで計算 → サーバーで検証・配置)
- **ミラー**: プレイヤーのブロック座標を中心に対称座標を生成

---

## 拡張のヒント

### 新しいビルドモードを追加する

1. `BuildMode.java` に新しい列挙値を追加
2. `BlockCalculator.java` の `calculate()` のswitch文にケースを追加
3. 計算メソッドを実装する

### Circle (円) モードの例

```java
private static void calculateCircle(List<BlockPos> positions, BlockPos center, int radius) {
    int y = center.getY();
    for (double angle = 0; angle < 360; angle += 1.0) {
        double rad = Math.toRadians(angle);
        int x = center.getX() + (int) Math.round(radius * Math.cos(rad));
        int z = center.getZ() + (int) Math.round(radius * Math.sin(rad));
        BlockPos pos = new BlockPos(x, y, z);
        if (!positions.contains(pos)) positions.add(pos);
    }
}
```