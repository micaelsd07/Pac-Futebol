@echo off
cd /d "%~dp0"
if exist PacFutebol.jar (
    echo Iniciando Pac-Futebol...
    start "" javaw -jar PacFutebol.jar
    exit /b 0
)
if not exist bin mkdir bin
echo Compilando PAC-FUTEBOL Java 2D...
javac -encoding UTF-8 -d bin src\*.java
if %errorlevel% neq 0 (
    echo Erro ao compilar o projeto!
    pause
    exit /b %errorlevel%
)
echo Iniciando PAC-FUTEBOL...
start "" javaw -cp bin Main
