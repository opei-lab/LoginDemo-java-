# LoginDemo Docker環境管理用Makefile

# デフォルトターゲット
.DEFAULT_GOAL := help

# 環境変数
DOCKER_COMPOSE := docker compose
APP_NAME := login-demo-app
GRADLE := ./gradlew

# カラー出力用
GREEN := \033[0;32m
RED := \033[0;31m
NC := \033[0m # No Color

.PHONY: help
help: ## ヘルプを表示
	@echo "LoginDemo Docker環境管理コマンド"
	@echo "================================"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "$(GREEN)%-15s$(NC) %s\n", $$1, $$2}'

.PHONY: build
build: ## アプリケーションをビルド（ローカル）
	@echo "$(GREEN)Gradleビルドを実行中...$(NC)"
	$(GRADLE) clean build

.PHONY: docker-build
docker-build: ## Dockerイメージをビルド
	@echo "$(GREEN)Dockerイメージをビルド中...$(NC)"
	$(DOCKER_COMPOSE) build --no-cache

.PHONY: up
up: ## コンテナを起動（バックグラウンド）
	@echo "$(GREEN)コンテナを起動中...$(NC)"
	$(DOCKER_COMPOSE) up -d
	@echo "$(GREEN)アプリケーション起動完了！$(NC)"
	@echo "URL: http://localhost:8081"
	@echo "H2 Console: http://localhost:8081/h2-console"

.PHONY: down
down: ## コンテナを停止・削除
	@echo "$(RED)コンテナを停止中...$(NC)"
	$(DOCKER_COMPOSE) down

.PHONY: restart
restart: down up ## コンテナを再起動

.PHONY: logs
logs: ## コンテナのログを表示
	$(DOCKER_COMPOSE) logs -f $(APP_NAME)

.PHONY: logs-all
logs-all: ## 全コンテナのログを表示
	$(DOCKER_COMPOSE) logs -f

.PHONY: ps
ps: ## コンテナの状態を確認
	$(DOCKER_COMPOSE) ps

.PHONY: exec
exec: ## アプリケーションコンテナに接続
	$(DOCKER_COMPOSE) exec app /bin/sh

.PHONY: clean
clean: ## Dockerリソースをクリーンアップ
	@echo "$(RED)Dockerリソースをクリーンアップ中...$(NC)"
	$(DOCKER_COMPOSE) down -v --rmi local

.PHONY: test
test: ## テストを実行
	@echo "$(GREEN)テストを実行中...$(NC)"
	$(GRADLE) test

.PHONY: test-docker
test-docker: ## Docker内でテストを実行
	@echo "$(GREEN)Docker内でテストを実行中...$(NC)"
	$(DOCKER_COMPOSE) exec app ./gradlew test

.PHONY: init
init: ## 初回セットアップ（ビルド→起動）
	@echo "$(GREEN)初回セットアップを開始...$(NC)"
	@make build
	@make docker-build
	@make up
	@echo "$(GREEN)セットアップ完了！$(NC)"

.PHONY: dev
dev: ## 開発モード（ログ表示付き起動）
	@echo "$(GREEN)開発モードで起動中...$(NC)"
	$(DOCKER_COMPOSE) up

.PHONY: health
health: ## ヘルスチェック
	@echo "$(GREEN)ヘルスチェック中...$(NC)"
	@curl -s http://localhost:8080/actuator/health | jq . || echo "$(RED)ヘルスチェック失敗$(NC)"

.PHONY: db-console
db-console: ## H2データベースコンソールを開く
	@echo "$(GREEN)H2コンソールを開いています...$(NC)"
	@echo "URL: http://localhost:8080/h2-console"
	@echo "JDBC URL: jdbc:h2:mem:testdb"
	@echo "Username: sa"
	@echo "Password: (空欄)"
	@open http://localhost:8080/h2-console || xdg-open http://localhost:8080/h2-console || echo "ブラウザで開いてください"

.PHONY: run-local
run-local: ## ローカル環境で実行（Dockerなし）
	@echo "$(GREEN)ローカル環境でアプリケーションを起動中...$(NC)"
	$(GRADLE) bootRun

.PHONY: ide
ide: ## IDE実行の説明を表示
	@echo "$(GREEN)=== IDE（IntelliJ IDEA/Eclipse）での実行方法 ===$(NC)"
	@echo ""
	@echo "1. IntelliJ IDEAの場合:"
	@echo "   - LoginDemoApplication.javaを右クリック"
	@echo "   - 'Run LoginDemoApplication'を選択"
	@echo ""
	@echo "2. Eclipseの場合:"
	@echo "   - LoginDemoApplication.javaを右クリック"
	@echo "   - Run As → Spring Boot Appを選択"
	@echo ""
	@echo "3. VS Codeの場合:"
	@echo "   - F5キーまたはRun → Start Debuggingを選択"
	@echo ""
	@echo "$(GREEN)起動後: http://localhost:8080 でアクセス可能$(NC)"