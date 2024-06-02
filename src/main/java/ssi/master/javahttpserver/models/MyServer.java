package ssi.master.javahttpserver.models;


import com.sun.net.httpserver.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;

public class MyServer {
    private static final int PORT = 8000;
    private HttpServer server;
    TextArea logs;
    private Set<String> blacklist;


    public MyServer(TextArea logs , Set<String> blacklist){
        this.logs = logs;
        this.blacklist = blacklist;
    }



    public void startServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", new MyHttpHandler(logs , blacklist));
            server.start();
            System.out.println("Server started on port " + PORT);
            System.out.println("Waiting for connections ...");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        if (server != null) {
            server.stop(0);
            System.out.println("Server stopped");
        }
    }

    public String getServerIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    public int getServerPort() {
        return PORT;
    }

}
