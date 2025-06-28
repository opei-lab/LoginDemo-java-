# テストとCI/CDガイド

## テスト実行方法

### ローカルでのテスト実行

```bash
# 全テストの実行
./gradlew test

# 特定のテストクラスの実行
./gradlew test --tests UserServiceImplTest

# カバレッジレポート生成
./gradlew test jacocoTestReport

# カバレッジレポートの確認
open build/reports/jacoco/test/html/index.html
```

### テストカバレッジ

現在のカバレッジ目標：
- 全体: 70%以上
- 新規/変更ファイル: 80%以上

除外対象：
- Entityクラス
- DTOクラス
- Configクラス
- Applicationクラス

## CI/CDパイプライン

### GitHub Actions ワークフロー

#### 1. CI Pipeline (`ci.yml`)
mainブランチへのpush時に実行：
- ✅ ユニットテスト実行
- ✅ カバレッジレポート生成
- ✅ セキュリティスキャン（Trivy, OWASP）
- ✅ コード品質チェック（SpotBugs, Checkstyle）
- ✅ Dockerイメージビルド

#### 2. PR Check (`pr-check.yml`)
プルリクエスト時に実行：
- ✅ テスト実行
- ✅ カバレッジ閾値チェック
- ✅ PRへのカバレッジコメント
- ✅ セキュリティチェック

### セキュリティスキャン

#### Trivy
- コンテナイメージの脆弱性スキャン
- ファイルシステムスキャン
- 結果はGitHub Security Advisoriesに統合

#### OWASP Dependency Check
- 依存関係の脆弱性チェック
- HTMLレポートを生成

## テスト戦略

### 1. ユニットテスト
- Service層: ビジネスロジックのテスト
- Controller層: エンドポイントのテスト
- Security: 認証・認可のテスト

### 2. 統合テスト
- `@SpringBootTest`: アプリケーション全体のテスト
- `@WebMvcTest`: Web層のテスト
- `@DataJpaTest`: データアクセス層のテスト

### 3. テストデータ
- `@Sql`: テストデータの投入
- `@DirtiesContext`: コンテキストのリセット

## ベストプラクティス

### テスト作成時の注意点
1. **AAA原則**: Arrange, Act, Assert
2. **独立性**: 各テストは独立して実行可能
3. **明確な名前**: 何をテストしているか分かる名前
4. **モック活用**: 外部依存はモック化

### CI/CD改善のヒント
1. **並列実行**: テストの並列化で高速化
2. **キャッシュ**: 依存関係のキャッシュ
3. **段階的デプロイ**: ステージング→本番
4. **ロールバック**: 自動ロールバック機能

## トラブルシューティング

### テストが失敗する場合
```bash
# 詳細なログを出力
./gradlew test --info

# 特定のテストのみ実行
./gradlew test --tests "*ServiceTest"

# キャッシュクリア
./gradlew clean test
```

### カバレッジが低い場合
1. 除外設定を確認
2. 未テストのクラスを特定
3. 統合テストを追加

## 今後の改善計画

- [ ] E2Eテストの追加（Selenium）
- [ ] パフォーマンステスト（JMeter）
- [ ] 変異テスト（PITest）
- [ ] テストコンテナ（Testcontainers）
- [ ] API契約テスト（Spring Cloud Contract）