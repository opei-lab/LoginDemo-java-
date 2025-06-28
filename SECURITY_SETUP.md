# セキュリティ設定ガイド

## 環境変数の設定

本アプリケーションのセキュリティを確保するため、以下の環境変数を設定してください。

### 必須の環境変数

1. **SECURITY_PEPPER**
   - パスワードハッシュ化時に使用するペッパー値
   - 例: `export SECURITY_PEPPER="mySecretPepper123!@#"`
   - 注意: 本番環境では強力なランダム文字列を使用してください

2. **JWT_SECRET**
   - JWT署名に使用する秘密鍵（将来の実装用）
   - 例: `export JWT_SECRET="myJwtSecret456$%^"`

### 設定方法

#### Linux/Mac
```bash
# ~/.bashrcまたは~/.zshrcに追加
export SECURITY_PEPPER="your-secret-pepper-here"
export JWT_SECRET="your-jwt-secret-here"

# 設定を反映
source ~/.bashrc
```

#### Windows (コマンドプロンプト)
```cmd
setx SECURITY_PEPPER "your-secret-pepper-here"
setx JWT_SECRET "your-jwt-secret-here"
```

#### Windows (PowerShell)
```powershell
[Environment]::SetEnvironmentVariable("SECURITY_PEPPER", "your-secret-pepper-here", "User")
[Environment]::SetEnvironmentVariable("JWT_SECRET", "your-jwt-secret-here", "User")
```

#### IDE設定（IntelliJ IDEA）
1. Run > Edit Configurations
2. Environment variablesに追加:
   ```
   SECURITY_PEPPER=your-secret-pepper-here;JWT_SECRET=your-jwt-secret-here
   ```

#### IDE設定（VS Code）
`.vscode/launch.json`に追加:
```json
{
    "configurations": [
        {
            "env": {
                "SECURITY_PEPPER": "your-secret-pepper-here",
                "JWT_SECRET": "your-jwt-secret-here"
            }
        }
    ]
}
```

### Docker使用時
```dockerfile
# Dockerfile
ENV SECURITY_PEPPER=${SECURITY_PEPPER}
ENV JWT_SECRET=${JWT_SECRET}
```

または

```yaml
# docker-compose.yml
services:
  app:
    environment:
      - SECURITY_PEPPER=${SECURITY_PEPPER}
      - JWT_SECRET=${JWT_SECRET}
```

## セキュリティベストプラクティス

1. **ペッパー値の管理**
   - 本番環境では最低32文字以上のランダム文字列を使用
   - 定期的に更新（年1回程度）
   - バージョン管理システムにコミットしない

2. **環境ごとの分離**
   - 開発、ステージング、本番で異なる値を使用
   - 環境変数管理ツール（AWS Secrets Manager、HashiCorp Vault等）の使用を推奨

3. **バックアップ**
   - ペッパー値を安全な場所にバックアップ
   - 紛失するとパスワード検証ができなくなるため注意

## トラブルシューティング

### ペッパー値が設定されていない場合
アプリケーションは`application.properties`のデフォルト値を使用しますが、セキュリティリスクがあります。
必ず環境変数を設定してください。

### パスワード検証に失敗する場合
1. 環境変数が正しく設定されているか確認
2. アプリケーション再起動後も環境変数が保持されているか確認
3. ペッパー値に特殊文字が含まれる場合は適切にエスケープされているか確認