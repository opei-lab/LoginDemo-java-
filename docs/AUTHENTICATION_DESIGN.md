# 認証識別子の設計パターン

## 現在の実装の前提

現在のシステムは**ニックネーム/ユーザーID方式**を採用しています：

```java
@Entity
public class User {
    @Column(unique = true, nullable = false)
    private String username;  // ユニークなユーザーID（例：@johndoe）
    
    @Column(unique = true, nullable = false)  
    private String email;     // メールアドレスも一意
}
```

## 各方式の比較

### 1. ニックネーム方式（現在の実装）

**適用例**: Twitter、GitHub、Instagram

```java
// 実装例
username: "tanaka_taro"  // ユニーク
email: "tanaka@example.com"
displayName: "田中太郎"  // 表示用（未実装）
```

**メリット**:
- 覚えやすいログインID
- プライバシー保護（本名を隠せる）
- URLに使いやすい（/users/tanaka_taro）

**デメリット**:
- 希望のIDが取得できない可能性
- 同姓同名の人が後から困る

### 2. メールアドレス方式

**適用例**: Amazon、楽天、多くの企業システム

```java
@Entity
public class User {
    @Id
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;  // ログインID兼連絡先
    
    @Column(nullable = false)
    private String fullName;  // 本名（重複可）
    
    @Column
    private String displayName;  // 表示名（重複可）
}
```

**実装変更例**:
```java
// AuthController.java
@PostMapping("/login")
public String login(@RequestParam String email,  // usernameではなくemail
                   @RequestParam String password) {
    // メールアドレスで認証
}

// UserRepository.java
Optional<User> findByEmail(String email);
```

### 3. 社員番号/学籍番号方式

**適用例**: 企業内システム、大学システム

```java
@Entity
public class User {
    @Column(unique = true, nullable = false)
    private String employeeId;  // "EMP001234"
    
    @Column(nullable = false)
    private String fullName;    // "田中太郎"（重複可）
    
    @Column(nullable = false)
    private String department;  // "営業部"
}
```

### 4. 複合方式（実名サービス向け）

**適用例**: Facebook、LinkedIn

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ログイン識別子（メールまたは電話番号）
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(unique = true)
    private String phoneNumber;
    
    // 表示情報（重複可）
    @Column(nullable = false)
    private String firstName;  // "太郎"
    
    @Column(nullable = false)
    private String lastName;   // "田中"
    
    @Column
    private String middleName;
    
    // プロフィールURL用（オプション）
    @Column(unique = true)
    private String profileUrl;  // "tanaka.taro.123"
}
```

## 推奨される設計改善

### Option 1: 最小限の変更（表示名追加）

```java
@Entity
public class User {
    // 既存フィールドはそのまま
    @Column(unique = true, nullable = false)
    private String username;  // ログインID
    
    // 表示名を追加
    @Column(nullable = false)
    private String displayName;  // "田中太郎"（重複可）
    
    @Column
    private String fullName;     // 本名（オプション）
}
```

### Option 2: メールログインへの移行

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;  // 主要なログインID
    
    @Column(unique = true)
    private String username;  // オプション（プロフィールURL用）
    
    @Column(nullable = false)
    private String displayName;
}

// SecurityConfigの変更
.formLogin(form -> form
    .loginPage("/login")
    .usernameParameter("email")  // emailフィールドを使用
    .passwordParameter("password")
)
```

### Option 3: 柔軟なログイン（ベストプラクティス）

```java
@Service
public class FlexibleAuthService {
    
    public User authenticate(String loginId, String password) {
        // メールアドレス形式かチェック
        if (loginId.contains("@")) {
            return authenticateByEmail(loginId, password);
        }
        // 電話番号形式かチェック
        else if (loginId.matches("^[0-9+\\-]+$")) {
            return authenticateByPhone(loginId, password);
        }
        // それ以外はユーザー名として扱う
        else {
            return authenticateByUsername(loginId, password);
        }
    }
}
```

## 移行戦略

既存システムから移行する場合：

```sql
-- 1. 表示名カラムを追加
ALTER TABLE users ADD COLUMN display_name VARCHAR(255);

-- 2. 既存のusernameから初期値を設定
UPDATE users SET display_name = username WHERE display_name IS NULL;

-- 3. 新規登録時は別々に入力させる
```

## 用途別推奨設計

| 用途 | 推奨識別子 | 理由 |
|-----|-----------|------|
| SNS・コミュニティ | username（ニックネーム） | 匿名性、覚えやすさ |
| ECサイト | email | 連絡先として必須、忘れにくい |
| 企業内システム | 社員番号 + email | 組織内で一意、退職者管理 |
| 実名SNS | email + 表示名 | 本人確認、同姓同名対応 |
| 開発者向けサービス | username | GitHubとの親和性 |

## 現在の実装を改善する場合

```java
// 最小限の変更で同姓同名に対応
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ログインIDとして維持（ユニーク）
    @Column(unique = true, nullable = false)
    private String username;
    
    // 表示名を追加（重複可）
    @Column(nullable = false)
    private String displayName;
    
    // 本名フィールド（オプション）
    @Column
    private String realName;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    // プロフィール画面用
    @Column
    private String bio;
    
    @Column
    private String profileImageUrl;
}
```

この設計により：
- 既存の認証ロジックは変更不要
- 同姓同名の人も異なるusernameで登録可能
- 表示上は本名や好きな名前を使える