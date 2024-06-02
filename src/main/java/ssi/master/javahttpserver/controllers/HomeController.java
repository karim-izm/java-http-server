package ssi.master.javahttpserver.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ssi.master.javahttpserver.models.MyServer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


public class HomeController {
    private MyServer myServer;
    @FXML
    private ImageView logo;

    @FXML
    private Label serverStatus;
    @FXML
    private Label serverIp;
    @FXML
    private Label serverPort;

    @FXML
    private TextArea logTextArea;

    @FXML
    private ListView<String> fileListView;
    @FXML
    private ListView<String> blacklistView;

    @FXML
    private Button startButton;

    @FXML
    private Button stopButton;

    @FXML
    private TextField ipTextField;

    private ObservableList<String> fileList = FXCollections.observableArrayList();
    private ObservableList<String> blacklist = FXCollections.observableArrayList();
    private Set<String> blacklistSet = new HashSet<>();




    private boolean status;


    @FXML
    private void initialize() {
        myServer = new MyServer(logTextArea , blacklistSet);
        Image image = new Image(getClass().getResourceAsStream("/ssi/master/javahttpserver/images/ensa-logo.png"));
        logo.setImage(image);
        logTextArea.setEditable(false);

        stopButton.setDisable(true);

        serverIp.setVisible(false);
        serverPort.setVisible(false);

        serverIp.setText("Server Ip : "+myServer.getServerIpAddress());
        serverPort.setText("Server Port : "+myServer.getServerPort());

        blacklistView.setItems(blacklist);
        blacklistView.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                String selectedIp = blacklistView.getSelectionModel().getSelectedItem();
                if (selectedIp != null) {
                    blacklist.remove(selectedIp);
                    blacklistSet.remove(selectedIp);
                    logTextArea.appendText(LocalDateTime.now() + " : Removed " + selectedIp + " from the blacklist\n");
                }
            }
        });

        updateFileListView();
    }



    @FXML
    private void handleOpenServer(ActionEvent event) {
        myServer.startServer();
        serverStatus.setText("Server Status : RUNNING .....");
        logTextArea.appendText(LocalDateTime.now()+" : Server started.\n");
        status = true;
        serverIp.setVisible(true);
        serverPort.setVisible(true);

        startButton.setDisable(true);
        stopButton.setDisable(false);

    }

    @FXML
    private void handleStopServer(ActionEvent event) {
        myServer.stopServer();
        serverStatus.setText("Server Status : STOPPED .....");
        logTextArea.appendText(LocalDateTime.now()+" : Server stopped.\n");
        status = false;
        serverIp.setVisible(false);
        serverPort.setVisible(false);

        stopButton.setDisable(true);
        startButton.setDisable(false);
    }

    @FXML
    private void handleOpenFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an HTML File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("HTML Files", "*.html", "*.htm")
        );
        Stage stage = (Stage) serverStatus.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            uploadFile(selectedFile);
            updateFileListView();
        }
    }

    private void uploadFile(File file) {
        try {
            Files.copy(file.toPath(), new File("html/" + file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            logTextArea.appendText(LocalDateTime.now()+" : File uploaded: " + file.getName()+"\n");
            System.out.println(" : File uploaded: " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    public void handleExportLogs(ActionEvent event) {
        String logs = logTextArea.getText();
        if (!logs.isEmpty()) {
            try {
                LocalDateTime currentTime = LocalDateTime.now();
                String fileName = "logs_" + currentTime.toString().replace(':', '-') + ".txt";
                BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
                writer.write(logs);
                writer.close();
                showAlert("Logs Exported", "Logs exported to: " + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateFileListView() {
        File folder = new File("html/");
        File[] listOfFiles = folder.listFiles();
        fileList.clear();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    fileList.add(file.getName());
                }
            }
        }
        fileListView.setItems(fileList);
    }

    @FXML
    private void handleFileListViewClicked() {
        String selectedFileName = fileListView.getSelectionModel().getSelectedItem();
        if (selectedFileName != null) {
            String fileUrl = "http://localhost:8000/" + selectedFileName;
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(fileUrl));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleBlacklistIp(ActionEvent event) {
        String ip = ipTextField.getText();
        if (ip != null && !ip.trim().isEmpty()) {
            if (!blacklistSet.contains(ip)) {
                blacklistSet.add(ip);
                blacklist.add(ip);
                logTextArea.appendText(LocalDateTime.now() + " : IP blacklisted: " + ip + "\n");
                ipTextField.clear();
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
