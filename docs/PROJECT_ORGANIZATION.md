# Spring Boot プロジェクトの整理術

## ファイル分割 ≠ マイクロサービス

### 現在の構造（モノリシック）
```
LoginDemo/  ← 1つのアプリケーション
├── controller/  （20+ files）
├── service/     （15+ files）
├── repository/  （10+ files）
├── entity/      （10+ files）
└── config/      （5+ files）
```

これは**モノリシック**（一枚岩）アーキテクチャです。
ファイルは多いですが、**1つのアプリケーション**として動作します。

### マイクロサービスだったら
```
auth-service/          ← 独立したアプリ1
├── AuthController.java
├── UserService.java
└── Dockerfile

mfa-service/           ← 独立したアプリ2
├── MfaController.java
├── TotpService.java
└── Dockerfile

email-service/         ← 独立したアプリ3
├── EmailController.java
├── EmailService.java
└── Dockerfile
```
各サービスが**別々のプロセス**として動作し、APIで通信します。

## Springの要素を整理

### 1. Spring Framework提供のアノテーション
```java
// 基本的なDIアノテーション
@Component      // 汎用コンポーネント
@Service        // ビジネスロジック層
@Repository     // データアクセス層
@Controller     // Web層
@Configuration  // 設定クラス

// DI関連
@Autowired      // 依存性注入（非推奨）
@Qualifier      // 複数Bean時の指定
@Value          // プロパティ値注入

// Web関連
@GetMapping     // GET リクエスト
@PostMapping    // POST リクエスト
@RequestParam   // URLパラメータ
@PathVariable   // URLパス変数
@RequestBody    // JSONボディ
@ResponseBody   // JSON レスポンス
```

### 2. Lombokのアノテーション（ボイラープレート削減）
```java
// よく使うLombok
@Data           // getter/setter/toString/equals/hashCode
@Getter         // getterのみ
@Setter         // setterのみ
@NoArgsConstructor   // デフォルトコンストラクタ
@AllArgsConstructor  // 全フィールドのコンストラクタ
@RequiredArgsConstructor  // finalフィールドのコンストラクタ
@Slf4j          // ログ用のlogger生成
@Builder        // ビルダーパターン
```

### 3. JPA/Hibernateのアノテーション
```java
// エンティティ定義
@Entity         // JPAエンティティ
@Table          // テーブル名指定
@Id             // 主キー
@GeneratedValue // 自動生成
@Column         // カラム定義
@OneToMany      // 1対多リレーション
@ManyToOne      // 多対1リレーション
@JoinColumn     // 外部キー
```

## 機能別ファイル整理

### 認証機能（コア）
```
認証の核心部分：
├── entity/User.java              // ユーザーエンティティ
├── repository/UserRepository.java // ユーザーDB操作
├── service/impl/UserServiceImpl.java // 認証ロジック
├── controller/AuthController.java  // ログイン・登録
└── config/SecurityConfig.java     // Spring Security設定
```

### MFA機能（追加機能）
```
二要素認証：
├── controller/MfaController.java  // MFA画面
├── service/TotpService.java       // TOTP生成・検証
└── entity/BackupCode.java         // バックアップコード
```

### 監査機能（補助機能）
```
ログ記録：
├── entity/AuditLog.java           // 監査ログ
├── repository/AuditLogRepository.java
└── service/AuditLogService.java
```

## 初学者向け：最小構成から始める

### Step 1: 最小限の認証
```java
// 必要なファイル：5つだけ！
User.java           // エンティティ
UserRepository.java // DB操作
UserService.java    // ビジネスロジック
AuthController.java // Webエンドポイント
SecurityConfig.java // 設定
```

### Step 2: 機能追加
```java
// パスワードポリシー追加
+ PasswordValidator.java
+ PasswordPolicyConfig.java

// ログイン履歴追加
+ LoginAttempt.java
+ LoginAttemptRepository.java
```

## プロジェクト構造の理解方法

### 1. エントリーポイントから追う
```java
@SpringBootApplication
public class LoginDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoginDemoApplication.class, args);
    }
}
```

### 2. 1つのリクエストフローを追跡
```
ブラウザ → AuthController → UserService → UserRepository → DB
         ↓                ↓              ↓                ↓
    @GetMapping    @Service      JpaRepository      H2
```

### 3. 依存関係を図示
```java
AuthController
    ↓ @RequiredArgsConstructor（Lombokが自動注入）
    UserService（インターフェース）
        ↓
        UserServiceImpl（実装）
            ↓ @RequiredArgsConstructor
            UserRepository
            PasswordEncoder
            AuditLogService
```

## ファイル数が多い理由

### 1. 単一責任原則（SOLID）
```java
// ❌ 悪い例：1つのクラスに全部
public class UserManager {
    // ユーザー管理も認証もパスワード検証も全部...
}

// ✅ 良い例：責任を分離
public class UserService { }      // ユーザー管理
public class AuthService { }      // 認証
public class PasswordValidator { } // パスワード検証
```

### 2. テストしやすさ
```java
// 各クラスが小さいので単体テストが書きやすい
@Test
void testPasswordValidation() {
    // PasswordValidatorだけをテスト
}
```

### 3. 拡張性
```java
// 新しい認証方式を追加するとき
+ OAuth2Service.java  // 既存コードに影響なし
```

## 実践的な読み方

### 初心者向けアプローチ
1. **README.md** を読む
2. **application.properties** で設定確認
3. **AuthController** のエンドポイント確認
4. **1つの機能**（例：ログイン）を最初から最後まで追う
5. **テストコード**を読んで使い方を理解

### ツールの活用
```bash
# ファイル構造を可視化
tree src/main/java/com/example/demo -I "*.class"

# 特定のアノテーションを検索
grep -r "@Service" src/

# 依存関係を確認
./gradlew dependencies
```

## まとめ：これはマイクロサービスではない

- **現在**: モノリシック（1つのアプリ、多数のファイル）
- **利点**: デプロイ簡単、デバッグしやすい、学習に最適
- **ファイル分割の理由**: 保守性、テスト性、拡張性

マイクロサービスは「複数の独立したアプリケーション」であり、
現在のような「1つのアプリ内でのファイル分割」とは別物です。