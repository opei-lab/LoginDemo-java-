# H2データベース実践ガイド

## H2コンソールを使ったデータ確認

### 1. H2コンソールにアクセス

1. アプリケーションを起動
   ```bash
   ./gradlew bootRun
   ```

2. ブラウザでアクセス
   ```
   http://localhost:8080/h2-console
   ```

3. 接続情報を入力
   - **JDBC URL**: `jdbc:h2:mem:testdb`
   - **User Name**: `sa`
   - **Password**: （空欄のまま）
   - **Connect** をクリック

### 2. 実際のテーブル確認方法

左側のツリーで `TESTDB` > `Tables` を展開すると、以下のテーブルが表示されます：

```
TESTDB
├── Tables
│   ├── USERS
│   ├── LOGIN_ATTEMPTS
│   ├── PASSWORD_HISTORY
│   ├── AUDIT_LOGS
│   ├── BACKUP_CODES
│   └── OAUTH2_USER_LINKS
```

### 3. よく使うSQL例

#### ユーザー一覧を確認
```sql
SELECT * FROM USERS;
```

#### 最近のログイン試行を確認
```sql
SELECT * FROM LOGIN_ATTEMPTS 
ORDER BY ATTEMPTED_AT DESC 
LIMIT 10;
```

#### アカウントロック状態を確認
```sql
SELECT USERNAME, ACCOUNT_NON_LOCKED, 
       (SELECT COUNT(*) FROM LOGIN_ATTEMPTS 
        WHERE USERNAME = U.USERNAME 
        AND SUCCESS = FALSE 
        AND ATTEMPTED_AT > DATEADD('MINUTE', -30, CURRENT_TIMESTAMP)) as RECENT_FAILURES
FROM USERS U;
```

#### 監査ログを時系列で確認
```sql
SELECT * FROM AUDIT_LOGS 
WHERE USERNAME = 'testuser' 
ORDER BY TIMESTAMP DESC;
```

### 4. データ投入の例

#### テストユーザーを手動で作成
```sql
-- パスワードは事前にBCryptで暗号化されたもの
INSERT INTO USERS (USERNAME, EMAIL, PASSWORD, ENABLED, ACCOUNT_NON_LOCKED) 
VALUES ('testuser', 'test@example.com', 
        '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 
        TRUE, TRUE);
```

#### ログイン履歴を手動で追加
```sql
INSERT INTO LOGIN_ATTEMPTS (USERNAME, IP_ADDRESS, SUCCESS, ATTEMPTED_AT) 
VALUES ('testuser', '127.0.0.1', TRUE, CURRENT_TIMESTAMP);
```

### 5. テーブル構造の詳細確認

特定のテーブルの構造を確認：
```sql
-- USERS テーブルの構造
SHOW COLUMNS FROM USERS;
```

結果例：
```
FIELD           | TYPE         | NULL | KEY | DEFAULT
----------------|--------------|------|-----|--------
ID              | BIGINT       | NO   | PRI | 
USERNAME        | VARCHAR(255) | NO   | UNI | 
PASSWORD        | VARCHAR(255) | NO   |     |
EMAIL           | VARCHAR(255) | NO   | UNI |
ENABLED         | BOOLEAN      | YES  |     | TRUE
ACCOUNT_NON_LOCKED | BOOLEAN   | YES  |     | TRUE
MFA_ENABLED     | BOOLEAN      | YES  |     | FALSE
```

### 6. JPA自動生成の確認

Spring BootのJPAがどのようなSQLを生成しているか確認：

1. `application.properties` で設定
   ```properties
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.format_sql=true
   ```

2. コンソールログで確認
   ```sql
   Hibernate: 
       select
           u1_0.id,
           u1_0.account_non_locked,
           u1_0.email,
           u1_0.enabled,
           u1_0.mfa_enabled,
           u1_0.mfa_secret,
           u1_0.password,
           u1_0.username 
       from
           users u1_0 
       where
           u1_0.username=?
   ```

### 7. トラブルシューティング

#### Q: テーブルが見つからない
A: アプリケーションが起動していることを確認。H2はインメモリDBなので、アプリケーション停止でデータが消えます。

#### Q: データが消えた
A: H2のインメモリモードでは、アプリケーション再起動でデータがリセットされます。永続化したい場合：
```properties
# ファイルベースのH2に変更
spring.datasource.url=jdbc:h2:file:./data/testdb
```

#### Q: 日本語が文字化けする
A: 接続URLに文字エンコーディングを追加：
```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MySQL
```

### 8. 開発時の便利な使い方

#### データのエクスポート
```sql
SCRIPT TO 'backup.sql';
```

#### データのインポート
```sql
RUNSCRIPT FROM 'backup.sql';
```

#### 全テーブルのレコード数確認
```sql
SELECT 'USERS' as TABLE_NAME, COUNT(*) as COUNT FROM USERS
UNION ALL
SELECT 'LOGIN_ATTEMPTS', COUNT(*) FROM LOGIN_ATTEMPTS
UNION ALL
SELECT 'AUDIT_LOGS', COUNT(*) FROM AUDIT_LOGS;
```