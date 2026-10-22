package ma.project.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import ma.project.model.ChatClient;
import ma.project.model.ClientConfig;
import ma.project.model.ClientListener;

/*
 The VIEW of the client (JavaFX). Two screens, both built with GridPane:
   1. login screen: enter a username (empty = READ-ONLY MODE)
   2. chat screen: status (circle + "Online"), messages, input field, SEND and LEAVE buttons

 It only talks to the model through ChatClient (calls) and ClientListener (callbacks).
 Platform.runLater(...): callbacks come from the network thread; JavaFX controls may only be
 changed from the JavaFX Application Thread.
 */
public class ClientApp extends Application implements ClientListener {

    private final ChatClient client = new ChatClient(this);
    private ClientConfig config;
    private Stage stage;

    // chat screen controls (fields because the callbacks update them)
    private final TextArea messagesArea = new TextArea();
    private final TextField inputField = new TextField();
    private final Button sendButton = new Button("SEND");
    private final Button leaveButton = new Button("LEAVE");
    private final Circle statusCircle = new Circle(7);
    private final Label statusLabel = new Label();

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        try {
            // command-line arguments first, then config.properties
            config = ClientConfig.resolve(getParameters().getRaw());
        } catch (Exception ex) {
            showError("Configuration error", ex.getMessage());
            Platform.exit();
            return;
        }

        stage.setTitle("TCPClient");
        stage.setOnCloseRequest(e -> {
            client.leave();
            Platform.exit();
            System.exit(0);
        });
        stage.setScene(buildLoginScene());
        stage.show();
    }

    /* ---------------- screen 1: login ---------------- */

    private Scene buildLoginScene() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("root-pane");
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(24));
        grid.setHgap(10);
        grid.setVgap(12);

        Label title = new Label("TCP Group Chat");
        title.getStyleClass().add("title");
        Label server = new Label("Server: " + config.getIp() + ":" + config.getPort());
        server.getStyleClass().add("hint");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        Button joinButton = new Button("JOIN");
        joinButton.getStyleClass().add("primary");
        joinButton.setDefaultButton(true); // ENTER = JOIN

        Label hint = new Label("Leave the username empty to join in READ-ONLY MODE");
        hint.getStyleClass().add("hint");

        grid.add(title, 0, 0, 2, 1);
        grid.add(server, 0, 1, 2, 1);
        grid.add(usernameField, 0, 2);
        grid.add(joinButton, 1, 2);
        grid.add(hint, 0, 3, 2, 1);

        joinButton.setOnAction(e -> join(usernameField.getText()));

        Scene scene = new Scene(grid, 420, 260);
        scene.getStylesheets().add(getClass().getResource("/client.css").toExternalForm());
        return scene;
    }

    private void join(String username) {
        try {
            client.connect(config.getIp(), config.getPort(), username);
            stage.setScene(buildChatScene());
        } catch (Exception ex) {
            showError("Cannot connect", "No server at " + config.getIp() + ":" + config.getPort()
                    + "\nStart TCPServer first.\n\n" + ex.getMessage());
        }
    }

    /* ---------------- screen 2: chat ---------------- */

    private Scene buildChatScene() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("root-pane");
        grid.setPadding(new Insets(14));
        grid.setHgap(8);
        grid.setVgap(10);

        // row 0: status indicator (circle + "Online") and identity
        statusCircle.setFill(Color.web("#22c55e"));
        statusLabel.setText("Online");
        statusLabel.getStyleClass().add("status-online");
        HBox status = new HBox(8, statusCircle, statusLabel);
        status.setAlignment(Pos.CENTER_LEFT);

        Label identity = new Label(client.isReadOnly() ? "READ-ONLY MODE" : "Logged in as " + client.getUsername());
        identity.getStyleClass().add(client.isReadOnly() ? "badge-readonly" : "identity");
        identity.setMinWidth(Region.USE_PREF_SIZE); // never truncate the label

        grid.add(status, 0, 0);
        grid.add(identity, 1, 0, 2, 1);
        GridPane.setHalignment(identity, javafx.geometry.HPos.RIGHT);

        // row 1: messages
        messagesArea.setEditable(false);
        messagesArea.setWrapText(true);
        grid.add(messagesArea, 0, 1, 3, 1);

        // row 2: input + SEND + LEAVE
        inputField.setPromptText("Type a message...   (commands: allUsers, end, bye)");
        sendButton.getStyleClass().add("primary");
        leaveButton.getStyleClass().add("danger");
        grid.add(inputField, 0, 2);
        grid.add(sendButton, 1, 2);
        grid.add(leaveButton, 2, 2);

        inputField.setOnAction(e -> sendTypedMessage()); // ENTER
        sendButton.setOnAction(e -> sendTypedMessage());
        leaveButton.setOnAction(e -> client.leave());

        // READ-ONLY MODE: the user can read, but cannot send
        if (client.isReadOnly()) {
            inputField.setDisable(true);
            inputField.setPromptText("READ-ONLY MODE: you cannot send messages");
            sendButton.setDisable(true);
        }

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, new ColumnConstraints(), new ColumnConstraints());
        RowConstraints r0 = new RowConstraints();
        RowConstraints r1 = new RowConstraints();
        r1.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(r0, r1, new RowConstraints());

        Scene scene = new Scene(grid, 560, 480);
        scene.getStylesheets().add(getClass().getResource("/client.css").toExternalForm());
        Platform.runLater(inputField::requestFocus);
        return scene;
    }

    private void sendTypedMessage() {
        client.send(inputField.getText());
        inputField.clear();
    }

    /* ---------------- ClientListener: called by the model ---------------- */

    @Override
    public void onMessage(String message) {
        Platform.runLater(() -> messagesArea.appendText(message + "\n"));
    }

    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> {
            messagesArea.appendText("--- " + reason + " ---\n");
            statusCircle.setFill(Color.web("#ef4444"));
            statusLabel.setText("Offline");
            statusLabel.getStyleClass().setAll("label", "status-offline");
            inputField.setDisable(true);
            sendButton.setDisable(true);
            leaveButton.setDisable(true);
        });
    }

    private void showError(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR, text);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
