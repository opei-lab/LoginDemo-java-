#!/bin/bash
# 開発環境での起動スクリプト
# モックメールサービスを使用してOTPをコンソールに表示します

echo "開発モードで起動します..."
echo "OTPコードはコンソールに表示されます。"
echo ""

./gradlew bootRun --args='--spring.profiles.active=dev'