
@echo off
REM
REM Updates the jar files from upstream at https://github.com/ChargeTimeEU/Java-OCA-OCPP.git
REM

echo #
echo # Clear 'lib' folder
echo #
del lib\*.jar

IF EXIST git (
    echo #
    echo # Update repository in directory 'git'
    echo #
    cd git
    git pull
    cd ..

) ELSE (
    echo #
    echo # Cloning repository to directory 'git'
    echo #
    git clone https://github.com/ChargeTimeEU/Java-OCA-OCPP.git git
)
cd git

echo #
echo # Compile
echo #
call gradlew build jar

echo #
echo # Copy jar files
echo #

copy ocpp-common\build\libs\*.jar ..\lib
copy ocpp-v1_6\build\libs\*.jar ..\lib
REM copy ocpp-v2_0\build\libs\*.jar ..\lib
copy OCPP-J\build\libs ..\lib