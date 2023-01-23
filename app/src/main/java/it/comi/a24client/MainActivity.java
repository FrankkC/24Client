package it.comi.a24client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.IntentService;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends Activity {
    TcpClient mTcpClient;
    TextView replyTextView;
    Button connectButton;
    Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        TextView textView;

        textView = new TextView(this);
        textView.setText("hostname / IP:");
        linearLayout.addView(textView);
        final EditText hostnameEditText = new EditText(this);
        hostnameEditText.setText("10.10.0.52");
        hostnameEditText.setSingleLine(true);
        linearLayout.addView(hostnameEditText);

        textView = new TextView(this);
        textView.setText("port:");
        linearLayout.addView(textView);
        final EditText portEditText = new EditText(this);
        portEditText.setText("80");
        portEditText.setSingleLine(true);
        linearLayout.addView(portEditText);

        textView = new TextView(this);
        textView.setText("data to send:");
        linearLayout.addView(textView);
        final EditText dataEditText = new EditText(this);
        dataEditText.setText("ECHO");
        linearLayout.addView(dataEditText);

        replyTextView = new TextView(this);
        final ScrollView replyTextScrollView = new ScrollView(this);
        replyTextScrollView.addView(replyTextView);


        connectButton = new Button(this);
        connectButton.setText("CONNECT");
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //sends the message to the server
                if (mTcpClient != null && mTcpClient.isConnected()) {
                    mTcpClient.stopClient();
                    mTcpClient = null;
                    setConnectedStatus(false);
                } else {
                    new ConnectTask().execute(hostnameEditText.getText().toString(), portEditText.getText().toString());
                }


            }
        });
        linearLayout.addView(connectButton);

        sendButton = new Button(this);
        sendButton.setText("send command");
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //sends the message to the server
                if (mTcpClient != null) {
                    Date currentTime = Calendar.getInstance().getTime();
                    String dataToSend = dataEditText.getText().toString();
                    mTcpClient.sendMessage(dataToSend);
                }

            }
        });
        sendButton.setEnabled(false);
        linearLayout.addView(sendButton);

        textView = new TextView(this);
        textView.setText("output:");
        linearLayout.addView(textView);
        linearLayout.addView(replyTextScrollView);

        this.setContentView(linearLayout);
    }

    @Override
    protected void onStop() {
        // call the superclass method first
        super.onStop();

        if (mTcpClient != null) {
            mTcpClient.stopClient();
        }

    }

    protected void setConnectedStatus(boolean connectedStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (connectedStatus) {
                    sendButton.setEnabled(true);
                    connectButton.setText("DISCONNECT");
                } else {
                    sendButton.setEnabled(false);
                    connectButton.setText("CONNECT");
                }
            }
        });
    }

    public class ConnectTask extends AsyncTask<String, String, TcpClient> {

        @Override
        protected TcpClient doInBackground(String... message) {

            //we create a TCPClient object
            mTcpClient = new TcpClient(
                new TcpClient.OnConnection() {
                    @Override
                    //here the messageReceived method is implemented
                    public void connected(String message) {
                        setConnectedStatus(true);
                    }

                    @Override
                    //here the messageReceived method is implemented
                    public void connectionFailed(String message) {
                        setConnectedStatus(false);
                    }
                },

                new TcpClient.OnMessageReceived() {
                    @Override
                    //here the messageReceived method is implemented
                    public void messageReceived(String message) {
                        //this method calls the onProgressUpdate
                        publishProgress(message);
                    }
                }
            );
            mTcpClient.run(message[0], message[1]);

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //response received from server
            Log.d("test", "response " + values[0]);
            //process server response here....
            replyTextView.append(values[0]);
        }

    }

}