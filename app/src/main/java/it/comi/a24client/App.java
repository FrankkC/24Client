package it.comi.a24client;

import android.app.Application;

/**
 * Application singleton that holds the shared TcpClient instance
 * so both MainActivity and FineTuneActivity can access it.
 */
public class App extends Application {

    private static App instance;
    private TcpClient tcpClient;

    // Listener for messages that should be routed to the current foreground activity
    private TcpClient.OnMessageReceived messageListener;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static App getInstance() {
        return instance;
    }

    public TcpClient getTcpClient() {
        return tcpClient;
    }

    public void setTcpClient(TcpClient client) {
        this.tcpClient = client;
    }

    public void setMessageListener(TcpClient.OnMessageReceived listener) {
        this.messageListener = listener;
    }

    public TcpClient.OnMessageReceived getMessageListener() {
        return messageListener;
    }
}
