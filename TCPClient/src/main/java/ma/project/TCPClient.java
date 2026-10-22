package ma.project;

import javafx.application.Application;
import ma.project.view.ClientApp;

/*
 Entry point:   java -jar TCPClient.jar <ServerIPAddress> <PortNumber>
           e.g. java -jar TCPClient.jar localhost 3000
 Without arguments, the server address is read from config.properties.

 Why a separate launcher class? An executable "fat" JAR cannot start a main class that
 extends javafx.application.Application directly. This plain class just launches the UI
 and passes the command-line arguments (read in ClientApp with getParameters()).
 */
public class TCPClient {
    public static void main(String[] args) {
        Application.launch(ClientApp.class, args);
    }
}
