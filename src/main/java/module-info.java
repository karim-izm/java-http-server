module ssi.master.javahttpserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.httpserver;
    requires java.desktop;

    opens ssi.master.javahttpserver to javafx.fxml;
    opens ssi.master.javahttpserver.controllers to javafx.fxml;
    exports ssi.master.javahttpserver;
    exports ssi.master.javahttpserver.controllers;
}