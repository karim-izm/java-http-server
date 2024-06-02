package ssi.master.javahttpserver.models;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import javafx.scene.control.TextArea;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MyHttpHandler implements HttpHandler {
    private static final String HTML_DIRECTORY = "html";
    private static final int MAX_REQUESTS = 10;
    private static final long TIME_WINDOW = TimeUnit.MINUTES.toMillis(1);
    private ConcurrentHashMap<String , List<Long>> requestTimes = new ConcurrentHashMap<>();

    private Set<String> blacklist;


    private TextArea logs;

    public MyHttpHandler(TextArea logs, Set<String> blacklist) {
        this.logs = logs;
        this.blacklist = blacklist;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        String requestPath = exchange.getRequestURI().getPath();

        String clientIp = exchange.getRemoteAddress().getAddress().toString().substring(1);

        System.out.println(clientIp + " has connected to the server");
        logs.appendText(LocalDateTime.now()+" : "+clientIp+" requested "+requestMethod+" "+requestPath+"\n");

        if (blacklist.contains(clientIp)) {
            serveForbiddenResponse(exchange);
            logs.appendText(LocalDateTime.now() + " : Connection attempt from blacklisted IP: " + clientIp + "\n");
            return;
        }


        if (rateLimitExceeded(clientIp)) {
            String response = "429 - Too Many Requests";
            exchange.sendResponseHeaders(429, response.getBytes().length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
            logs.appendText(LocalDateTime.now()+" : "+clientIp+" Too Many Requests 429 "+requestMethod+" "+requestPath+"\n");

            return;
        }

        if (requestMethod.equalsIgnoreCase("GET") && requestPath.equals("/")) {
            listFiles(exchange);
        } else {
            serveFile(exchange, requestPath);
        }
    }


    private void listFiles(HttpExchange exchange) throws IOException {
        List<String> files = getFilesList();
        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("<html><body><h2>Available Files On The Server:</h2><ul>");
        for (String file : files) {
            responseBuilder.append("<li><a href=\"/" + file + "\">" + file + "</a></li>");
        }
        responseBuilder.append("</ul></body></html>");

        String response = responseBuilder.toString();
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.getBytes().length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.close();
    }

    private List<String> getFilesList() {
        List<String> files = new ArrayList<>();
        File folder = new File(HTML_DIRECTORY);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    files.add(file.getName());
                }
            }
        }
        return files;
    }

    private void serveFile(HttpExchange exchange, String requestPath) throws IOException {
        Path filePath = Paths.get(HTML_DIRECTORY, requestPath);
        File file = filePath.toFile();
        if (file.exists() && file.isFile()) {
            try (InputStream inputStream = new FileInputStream(file)) {
                byte[] bytes = inputStream.readAllBytes();
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(bytes);
                outputStream.close();
            }
        } else {
            String response = "404 - File not found";
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, response.getBytes().length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }
    }
        private boolean rateLimitExceeded(String clientIp) {
            long now = System.currentTimeMillis();
            requestTimes.putIfAbsent(clientIp, new ArrayList<>());
            List<Long> times = requestTimes.get(clientIp);
            times.removeIf(time -> time < now - TIME_WINDOW);
            if (times.size() >= MAX_REQUESTS) {
                return true;
            }
            times.add(now);
            return false;
        }

    private void serveForbiddenResponse(HttpExchange exchange) throws IOException {
        String response = "403 - Forbidden";
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_FORBIDDEN, response.getBytes().length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.close();
    }


}
