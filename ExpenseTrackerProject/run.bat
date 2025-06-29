@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
set MODULE_PATH=target/classes

java --module-path "%PATH_TO_FX%" --add-modules=javafx.controls,javafx.fxml -cp "target/classes;target/dependency/*" main.Main
