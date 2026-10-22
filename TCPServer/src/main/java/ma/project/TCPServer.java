package ma.project;

import javafx.application.Application;
import ma.project.view.ServerApp;

/*
 Entry point:   java -jar TCPServer.jar

 Why a separate launcher class? An executable "fat" JAR cannot start a main class that
 extends javafx.application.Application directly. This plain class just launches the UI.
 The IP address and port are read from config.properties (see ServerConfig).
 */
public class TCPServer {
    public static void main(String[] args) {
        Application.launch(ServerApp.class, args);
    }
}
