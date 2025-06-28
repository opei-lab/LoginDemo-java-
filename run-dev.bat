@echo off
REM 開発環境での起動スクリプト
REM モックメールサービスを使用してOTPをコンソールに表示します

echo 開発モードで起動します...
echo OTPコードはコンソールに表示されます。
echo.

gradlew.bat bootRun --args="--spring.profiles.active=dev"