package ma.project.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import ma.project.model.ChatServer;
import ma.project.model.ServerConfig;
import ma.project.model.ServerListener;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/*
 The VIEW of the server (JavaFX). It only DISPLAYS what the model reports through ServerListener.
 All socket / thread logic lives in ma.project.model.

 Platform.runLater(...): the model calls the listener from network threads, but JavaFX controls
 may only be modified from the JavaFX Application Thread. runLater() puts the update in its queue.
 */
public class ServerApp extends Application implements ServerListener {
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TextArea logArea = new TextArea();
    private final ObservableList<String> users = FXCollections.observableArrayList();
    private final Label usersTitle = new Label("Connected users (0)");
    private final Label statusLabel = new Label("Starting ...");

    // Visual distinction: each username gets a random background color
    private final Map<String, String> userColors = new HashMap<>();
    private final Random random = new Random();

    private ChatServer server;

    @Override
    public void start(Stage stage) {
        // ----- layout: GridPane (2 columns, 3 rows) -----
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(16));
        grid.setHgap(12);
        grid.setVgap(10);
        grid.getStyleClass().add("root-pane");

        Label title = new Label("TCP Chat Server");
        title.getStyleClass().add("title");
        statusLabel.getStyleClass().add("status");
        grid.add(title, 0, 0);
        grid.add(statusLabel, 1, 0);

        Label logTitle = new Label("Activity log");
        logTitle.getStyleClass().add("section");
        usersTitle.getStyleClass().add("section");
        grid.add(logTitle, 0, 1);
        grid.add(usersTitle, 1, 1);

        logArea.setEditable(false);
        logArea.setWrapText(true);
        grid.add(logArea, 0, 2);

        ListView<String> usersList = new ListView<>(users);
        usersList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(name);
                    setStyle("-fx-background-color: " + userColors.get(name) + "; -fx-text-fill: #1f2937;");
                }
            }
        });
        grid.add(usersList, 1, 2);

        ColumnConstraints logCol = new ColumnConstraints();
        logCol.setHgrow(Priority.ALWAYS);
        ColumnConstraints usersCol = new ColumnConstraints(220);
        grid.getColumnConstraints().addAll(logCol, usersCol);
        RowConstraints r0 = new RowConstraints();
        RowConstraints r1 = new RowConstraints();
        RowConstraints r2 = new RowConstraints();
        r2.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(r0, r1, r2);

        Scene scene = new Scene(grid, 760, 480);
        scene.getStylesheets().add(getClass().getResource("/server.css").toExternalForm());
        stage.setTitle("TCPServer");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            if (server != null) {
                server.stop();
            }
            Platform.exit();
            System.exit(0);
        });
        stage.show();

        // ----- start the model -----
        try {
            ServerConfig config = ServerConfig.load();
            statusLabel.setText("● Running on " + config.getIp() + ":" + config.getPort());
            server = new ChatServer(config, this);
            server.start();
        } catch (Exception ex) {
            statusLabel.setText("● Stopped");
            onLog("Cannot start the server: " + ex.getMessage());
        }
    }

    /* ---------------- ServerListener: called by the model ---------------- */

    @Override
    public void onLog(String message) {
        Platform.runLater(() -> logArea.appendText("[" + LocalTime.now().format(LOG_TIME) + "] " + message + "\n"));
    }

    @Override
    public void onUserJoined(String username) {
        Platform.runLater(() -> {
            userColors.put(username, randomPastelColor());
            users.add(username);
            usersTitle.setText("Connected users (" + users.size() + ")");
        });
    }

    @Override
    public void onUserLeft(String username) {
        Platform.runLater(() -> {
            users.remove(username);
            userColors.remove(username);
            usersTitle.setText("Connected users (" + users.size() + ")");
        });
    }

    // light random color (HSB: random hue, low saturation, high brightness) -> "#rrggbb"
    private String randomPastelColor() {
        Color c = Color.hsb(random.nextInt(360), 0.35, 0.97);
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }
}
