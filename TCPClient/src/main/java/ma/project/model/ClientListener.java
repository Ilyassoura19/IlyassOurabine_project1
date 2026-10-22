package ma.project.model;

/*
 The bridge between the Model and the View (Model-View decoupling).
 ChatClient never imports JavaFX: it only calls these methods. The view decides how to show them.
 */
public interface ClientListener {

    // a message arrived from the server (chat message, welcome, user list, ...)
    void onMessage(String message);

    // the connection is closed (user left, or the server is gone)
    void onDisconnected(String reason);
}
