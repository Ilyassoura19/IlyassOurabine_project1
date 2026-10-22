package ma.project.model;

/*
 The bridge between the Model and the View (Model-View decoupling).

 The model (ChatServer, ClientHandler) never imports JavaFX. It only calls these methods
 to say "something happened". Any view (JavaFX, console, tests...) can implement this
 interface and decide how to display it. Changing the UI never requires changing the model.
 */
public interface ServerListener {

    // an activity to show in the server log ("Server Started", "Waiting for Client", ...)
    void onLog(String message);

    // a named user joined -> add to the list of connected users
    void onUserJoined(String username);

    // a named user left -> remove from the list of connected users
    void onUserLeft(String username);
}
