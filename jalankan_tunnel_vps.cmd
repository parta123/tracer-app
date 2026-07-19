@echo off
title Tracer - Tunnel Database VPS (jangan ditutup)
echo Tunnel database VPS sedang dijalankan. Biarkan jendela ini terbuka.
ssh -N -L 127.0.0.1:13306:127.0.0.1:3306 -i "%~dp0asyscntr-db.pem" -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o ExitOnForwardFailure=yes asyscntr@103.127.133.81
echo.
echo Tunnel terputus. Jalankan file ini lagi untuk menyambungkan ulang.
pause
