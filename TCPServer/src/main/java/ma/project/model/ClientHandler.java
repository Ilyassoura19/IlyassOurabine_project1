package ma.project.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/*
 Serves ONE client. Same role as ClientHandlerThread ("one_thread_per_client") and
 WorkerThread ("thread-pool_model"): it implements Runnable, so it can be executed
 by a virtual thread -> Thread.ofVirtual().start(clientHandler).

 Protocol (DataInputStream.readUTF / DataOutputStream.writeUTF, like in the exercises):
   1. the FIRST message sent by the client is its username ("" = READ-ONLY MODE)
   2. every next message is either:
        - "allUsers"      -> the list of active users is sent back to THIS client only
        - "end" / "bye"   -> the connection is closed
        - anything else   -> formatted "[time] username: message" and broadcast to ALL clients
 */
public class ClientHandler implements Runnable {
    private final Socket socketForClient;
    private final ChatServer server;

    private DataOutputStream dout;
    private String username;
    private boolean readOnly;

    public ClientHandler(Socket socketForClient, ChatServer server) {
        this.socketForClient = socketForClient;
        this.server = server;
    }

    @Override
    public void run() {
        try (DataInputStream dis = new DataInputStream(socketForClient.getInputStream());
             DataOutputStream dout = new DataOutputStream(socketForClient.getOutputStream())) {
            this.dout = dout;

            // 1. Authentication & identity: the first message is the username
            String requestedName = dis.readUTF().trim();
            readOnly = requestedName.isEmpty();
            username = server.register(this, readOnly ? "guest" : requestedName);

            if (readOnly) {
                server.log("A guest joined in READ-ONLY MODE (" + socketForClient.getRemoteSocketAddress() + ")");
                send("Welcome! You are in READ-ONLY MODE: you can read messages but not send them.");
            } else {
                server.log("Welcome " + username);
                send("Welcome " + username + "!");
                server.broadcast(">> " + username + " joined the chat");
            }

            // 2. Chat loop
            while (true) {
                String msg = dis.readUTF(); // blocking method: waits for the next message
                server.log("Client " + username + " said: " + msg);

                if ("end".equalsIgnoreCase(msg) || "bye".equalsIgnoreCase(msg)) {
                    break;
                }

                if ("allUsers".equalsIgnoreCase(msg)) {
                    // reply to THIS client only (no broadcast)
                    send("Active users: " + String.join(", ", server.getActiveUsernames()));
                    continue;
                }

                if (readOnly) {
                    // the client UI already blocks sending; the server checks again (never trust the client)
                    send("READ-ONLY MODE: you cannot send messages.");
                    continue;
                }

                server.broadcast(server.format(username, msg));
            }

        } catch (IOException e) {
            // EOFException / SocketException: the client closed the connection without saying "bye"
        } finally {
            server.unregister(this);
            close();
            if (username != null && !readOnly) {
                server.log(username + " left the chat");
                server.broadcast("<< " + username + " left the chat");
            } else if (username != null) {
                server.log("A read-only guest left");
            }
        }
    }

    // synchronized: several virtual threads may broadcast to this client at the same moment,
    // their messages must not be mixed on the same output stream
    synchronized void send(String message) {
        try {
            dout.writeUTF(message);
            dout.flush();
        } catch (IOException e) {
            // client already disconnected: its own thread will clean up
        }
    }

    void close() {
        try {
            socketForClient.close();
        } catch (IOException ignored) {
        }
    }

    String getUsername() {
        return username;
    }

    boolean isReadOnly() {
        return readOnly;
    }
}
