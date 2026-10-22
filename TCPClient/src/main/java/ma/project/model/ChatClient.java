package ma.project.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/*
 The MODEL of the client: socket communication. No JavaFX here.

 Same building blocks as the "tcp_socket_client" exercise:
     new Socket(ip, port) + DataInputStream.readUTF() + DataOutputStream.writeUTF()

 What changes: in the exercise the client is "ping-pong" (send, then WAIT for the reply).
 In a chat, messages from other users can arrive at ANY time, even while we are typing.
 So reading happens in a separate thread (receiveMessages) that loops on readUTF() forever,
 while sending happens whenever the user presses SEND.
 */
public class ChatClient {
    private final ClientListener listener;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dout;

    private String username;
    private boolean readOnly;
    private boolean connected = false;

    public ChatClient(ClientListener listener) {
        this.listener = listener;
    }

    // Connects to the server and sends the username as the FIRST message ("" = READ-ONLY MODE)
    public void connect(String ip, int port, String username) throws IOException {
        socket = new Socket(ip, port);
        dis = new DataInputStream(socket.getInputStream());
        dout = new DataOutputStream(socket.getOutputStream());

        this.username = username.trim();
        this.readOnly = this.username.isEmpty();

        dout.writeUTF(this.username);
        dout.flush();
        connected = true;

        // listener thread: always reading, so the user can receive while typing
        Thread.ofVirtual().start(this::receiveMessages);
    }

    private void receiveMessages() {
        try {
            while (true) {
                String msg = dis.readUTF(); // blocking method
                listener.onMessage(msg);
            }
        } catch (IOException e) {
            // EOFException / SocketException: the connection is closed
        }
        disconnect("Disconnected from the server.");
    }

    // Sends a message typed by the user. "end" / "bye" also close the connection.
    public void send(String msg) {
        msg = msg.trim();
        if (!isConnected() || msg.isEmpty()) {
            return;
        }
        boolean exit = "end".equalsIgnoreCase(msg) || "bye".equalsIgnoreCase(msg);

        if (readOnly && !exit) {
            listener.onMessage("READ-ONLY MODE: you cannot send messages.");
            return;
        }

        try {
            dout.writeUTF(msg);
            dout.flush();
        } catch (IOException e) {
            disconnect("Connection lost: " + e.getMessage());
            return;
        }

        if (exit) {
            disconnect("You left the chat.");
        }
    }

    // LEAVE button / window closed: notify the server, then close
    public void leave() {
        if (isConnected()) {
            try {
                dout.writeUTF("bye");
                dout.flush();
            } catch (IOException ignored) {
            }
            disconnect("You left the chat.");
        }
    }

    // synchronized + "if (!connected) return": the listener thread and the UI thread can both
    // try to disconnect at the same time; the view must be notified only ONCE
    private synchronized void disconnect(String reason) {
        if (!connected) {
            return;
        }
        connected = false;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        listener.onDisconnected(reason);
    }

    public synchronized boolean isConnected() {
        return connected;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public String getUsername() {
        return username;
    }
}
