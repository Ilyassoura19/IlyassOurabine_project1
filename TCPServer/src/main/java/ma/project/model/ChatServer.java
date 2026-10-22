package ma.project.model;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 The MODEL of the server: sockets + threads + chat logic. No JavaFX here.

 Same structure as the "one_thread_per_client" exercise (TcpServer + ClientHandlerThread),
 but each client is served by a VIRTUAL THREAD (exercise "one_virtual_thread_per_client"):
     Socket socketForClient = serverSocket.accept();
     Thread.ofVirtual().start(clientHandler);

 New compared to the exercises: the server keeps the list of ALL connected clients so it can
 BROADCAST a message received from one client to everybody.
 */
public class ChatServer {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final ServerConfig config;
    private final ServerListener listener;

    // Thread-safe list: many virtual threads add / remove / loop over it at the same time
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public ChatServer(ServerConfig config, ServerListener listener) {
        this.config = config;
        this.listener = listener;
    }

    // Starts the accept loop in a background thread, so the caller (e.g. the JavaFX UI) is not blocked
    public void start() {
        running = true;
        Thread acceptThread = new Thread(this::acceptClients, "accept-thread");
        acceptThread.setDaemon(true); // does not keep the JVM alive when the window is closed
        acceptThread.start();
    }

    private void acceptClients() {
        try (ServerSocket serverSocket = new ServerSocket(config.getPort(), 50, InetAddress.getByName(config.getIp()))) {
            this.serverSocket = serverSocket;
            listener.onLog("Server Started on " + config.getIp() + ":" + config.getPort());

            while (running) {
                // waiting for clients
                listener.onLog("Waiting for Client ...");
                Socket socketForClient = serverSocket.accept(); // blocking method
                listener.onLog("New connection accepted: " + socketForClient.getRemoteSocketAddress());

                // Start a service thread: one virtual thread per client
                ClientHandler clientHandler = new ClientHandler(socketForClient, this);
                Thread.ofVirtual().start(clientHandler);
            }
        } catch (IOException ex) {
            if (running) {
                listener.onLog("Server error: " + ex.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        for (ClientHandler client : clients) {
            client.close();
        }
        try {
            if (serverSocket != null) {
                serverSocket.close(); // unblocks accept()
            }
        } catch (IOException ignored) {
        }
        listener.onLog("Server Stopped");
    }

    /* ---------------- methods used by the ClientHandler threads ---------------- */

    // Adds the client to the list. Returns the final username (made unique if already taken).
    // synchronized: two clients choosing the same name at the same time cannot both get it.
    synchronized String register(ClientHandler client, String requestedName) {
        String finalName = requestedName;
        if (!client.isReadOnly()) {
            int n = 2;
            while (getActiveUsernames().contains(finalName)) {
                finalName = requestedName + n++;
            }
        }
        clients.add(client);
        if (!client.isReadOnly()) {
            listener.onUserJoined(finalName);
        }
        return finalName;
    }

    void unregister(ClientHandler client) {
        if (clients.remove(client) && !client.isReadOnly()) {
            listener.onUserLeft(client.getUsername());
        }
    }

    // Message distribution: send the same message to every connected client
    void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    // "[14:05] Ilyass: hello"
    String format(String username, String message) {
        return "[" + LocalTime.now().format(TIME_FORMAT) + "] " + username + ": " + message;
    }

    // Names of the active (non read-only) users
    List<String> getActiveUsernames() {
        List<String> names = new ArrayList<>();
        for (ClientHandler client : clients) {
            if (!client.isReadOnly()) {
                names.add(client.getUsername());
            }
        }
        return names;
    }

    void log(String message) {
        listener.onLog(message);
    }
}
