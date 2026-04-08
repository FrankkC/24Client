package it.comi.a24client;

import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TcpClient {

    public static final String TAG = TcpClient.class.getSimpleName();
    public static final String SERVER_IP = "192.168.1.81"; //server IP address
    public static final int SERVER_PORT = 80;
    // message to send to the server
    private String mServerMessage;

    // sends message received notifications
    private OnConnection mConnectionListener = null;

    // sends message received notifications
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;

    private Socket socket;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TcpClient(OnConnection connectionListener, OnMessageReceived messageListener) {
        mConnectionListener = connectionListener;
        mMessageListener = messageListener;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(final String message) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mBufferOut != null) {
                    Log.d(TAG, "Sending: " + message);
                    mBufferOut.println(message);
                    mBufferOut.flush();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {

        mRun = false;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }

        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public static boolean canConnect(String hostname, int port, int timeoutMs) {
        Socket probe = new Socket();
        try {
            probe.connect(new InetSocketAddress(hostname, port), timeoutMs);
            return true;
        } catch (IOException ignored) {
            return false;
        } finally {
            try {
                probe.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void run(String hostname, String port) {

        mRun = true;

        Socket localSocket = null;

        try {
            InetAddress serverAddr = InetAddress.getByName(hostname);
            int portInt = Integer.parseInt(port);
            Log.d("TCP Client", "C: Connecting...");

            try {
                localSocket = new Socket(serverAddr, portInt);
                this.socket = localSocket;

                if (localSocket.isConnected()) {
                    Log.d("socketDebug","connected");
                } else {
                    Log.d("socketDebug","NOT connected");
                }

                mConnectionListener.connected("");
            } catch (UnknownHostException e) {
                mConnectionListener.connectionFailed("");
                return;
            } catch (IOException e) {
                mConnectionListener.connectionFailed("");
                return;
            }

            try {
                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(localSocket.getOutputStream())), true);
                mBufferIn = new BufferedReader(new InputStreamReader(localSocket.getInputStream()));

                while (mRun) {
                    mServerMessage = mBufferIn.readLine();
                    if (mServerMessage == null) {
                        break;
                    }
                    if (mServerMessage != null && mMessageListener != null) {
                        mMessageListener.messageReceived(mServerMessage);
                    }
                }

                Log.d("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");
            } catch (Exception e) {
                Log.e("TCP", "S: Error", e);
            } finally {
                try {
                    if (localSocket != null && !localSocket.isClosed()) {
                        localSocket.close();
                    }
                } catch (IOException ignored) {
                }
                socket = null;
            }

        } catch (Exception e) {
            Log.e("TCP", "C: Error", e);
            mConnectionListener.connectionFailed("");
        }

    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the Activity
    //class at on AsyncTask doInBackground
    public interface OnConnection {
        public void connected(String message);
        public void connectionFailed(String message);
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the Activity
    //class at on AsyncTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }

}