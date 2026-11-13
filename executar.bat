@echo off
cd /d "%~dp0"
if not exist bin mkdir bin
echo Compilando PAC-MAN Java 2D...
javac -encoding UTF-8 -d bin src\*.java
if %errorlevel% neq 0 (
    echo Erro ao compilar o projeto!
    pause
    exit /b %errorlevel%
)
echo Iniciando PAC-MAN...
java -cp bin Main
