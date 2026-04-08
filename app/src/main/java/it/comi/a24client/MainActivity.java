package it.comi.a24client;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "it.comi.a24client.network";
    private static final String KEY_LAST_HOST = "last_host";
    private static final String DEFAULT_HOST = "24clocks-master.local";
    private static final int DEFAULT_TCP_PORT = 23;
    private static final int PROBE_CONNECT_TIMEOUT_MS = 700;

    private TcpClient mTcpClient;
    private TextView outputTextView;
    private ScrollView outputScrollView;
    private Button connectButton;
    private Button sendButton;
    private EditText dataEditText;
    private Button finetuneButton;
    private EditText hostnameEditText;

    // Quick command buttons that need enable/disable
    private Button[] commandButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);

        // === ROW 1: IP + CONNECT ===
        LinearLayout ipRow = new LinearLayout(this);
        ipRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(ipRow);

        hostnameEditText = new EditText(this);
        String lastHost = loadCachedHost();
        hostnameEditText.setText(lastHost.isEmpty() ? DEFAULT_HOST : lastHost);
        hostnameEditText.setSingleLine(true);
        hostnameEditText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        ipRow.addView(hostnameEditText);

        connectButton = new Button(this);
        connectButton.setText("CONNECT");
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTcpClient != null && mTcpClient.isConnected()) {
                    mTcpClient.stopClient();
                    mTcpClient = null;
                    App.getInstance().setTcpClient(null);
                    setConnectedStatus(false);
                } else {
                    new ConnectTask().execute(hostnameEditText.getText().toString(), String.valueOf(DEFAULT_TCP_PORT));
                }
            }
        });
        ipRow.addView(connectButton);

        // === ROW 2: Quick Commands label ===
        TextView cmdLabel = new TextView(this);
        cmdLabel.setText("Comandi:");
        cmdLabel.setPadding(0, 16, 0, 4);
        root.addView(cmdLabel);

        // === ROW 3: Quick command buttons - row 1 ===
        LinearLayout cmdRow1 = new LinearLayout(this);
        cmdRow1.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(cmdRow1);

        Button btnSetntp = makeCommandButton("SETNTP", "SETNTP");
        Button btnSethome = makeCommandButton("SETHOME", "SETHOME");
        Button btnSetzero = makeCommandButton("SETZERO", "SETZERO");
        Button btnGetpos = makeCommandButton("GETPOS", "GETPOS");

        cmdRow1.addView(btnSetntp, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        cmdRow1.addView(btnSethome, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        cmdRow1.addView(btnSetzero, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        cmdRow1.addView(btnGetpos, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // === ROW 4: Quick command buttons - row 2 ===
        LinearLayout cmdRow2 = new LinearLayout(this);
        cmdRow2.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(cmdRow2);

        Button btnLedOn = makeCommandButton("LED ON", "SETLED=1");
        Button btnLedOff = makeCommandButton("LED OFF", "SETLED=0");
        Button btnDebug = makeCommandButton("DEBUG", "DEBUG");
        Button btnUptime = makeCommandButton("UPTIME", "UPTIME");

        cmdRow2.addView(btnLedOn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        cmdRow2.addView(btnLedOff, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        cmdRow2.addView(btnDebug, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        cmdRow2.addView(btnUptime, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        commandButtons = new Button[]{btnSetntp, btnSethome, btnSetzero, btnGetpos, btnLedOn, btnLedOff, btnDebug, btnUptime};

        // === ROW 5: FINETUNE button ===
        finetuneButton = new Button(this);
        finetuneButton.setText("FINETUNE \u25B6");
        finetuneButton.setEnabled(false);
        finetuneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FineTuneActivity.class);
                startActivity(intent);
            }
        });
        root.addView(finetuneButton);

        // === ROW 6: Free text + SEND ===
        LinearLayout sendRow = new LinearLayout(this);
        sendRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(sendRow);

        dataEditText = new EditText(this);
        dataEditText.setHint("Comando...");
        dataEditText.setSingleLine(true);
        dataEditText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        sendRow.addView(dataEditText);

        sendButton = new Button(this);
        sendButton.setText("SEND");
        sendButton.setEnabled(false);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTcpClient != null) {
                    String cmd = dataEditText.getText().toString();
                    mTcpClient.sendMessage(cmd);
                    appendOutput("> " + cmd);
                }
            }
        });
        sendRow.addView(sendButton);

        // === ROW 7: Output header + clear button ===
        LinearLayout outputHeaderRow = new LinearLayout(this);
        outputHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        outputHeaderRow.setPadding(0, 16, 0, 4);
        root.addView(outputHeaderRow);

        TextView outputLabel = new TextView(this);
        outputLabel.setText("Output:");
        outputLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        outputHeaderRow.addView(outputLabel);

        Button clearOutputButton = new Button(this);
        clearOutputButton.setText("CLEAR");
        clearOutputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                outputTextView.setText("");
            }
        });
        outputHeaderRow.addView(clearOutputButton);

        outputScrollView = new ScrollView(this);
        outputScrollView.setBackgroundColor(Color.parseColor("#1a1a2e"));
        outputScrollView.setPadding(8, 8, 8, 8);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        root.addView(outputScrollView, scrollParams);

        outputTextView = new TextView(this);
        outputTextView.setTextColor(Color.parseColor("#00ff88"));
        outputTextView.setTextSize(12);
        outputScrollView.addView(outputTextView);

        setContentView(root);

        // Disable all command buttons initially
        setConnectedStatus(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear the app-level listener: MainActivity handles output via publishProgress directly
        App.getInstance().setMessageListener(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing() && mTcpClient != null) {
            mTcpClient.stopClient();
        }
    }

    private Button makeCommandButton(String label, final String command) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(11);
        btn.setPadding(4, 4, 4, 4);
        btn.setEnabled(false);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(command);
                    appendOutput("> " + command);
                }
            }
        });
        return btn;
    }

    private void appendOutput(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                outputTextView.append(text + "\n");
                outputScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        outputScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    private void setConnectedStatus(final boolean connected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectButton.setText(connected ? "DISCONNECT" : "CONNECT");
                sendButton.setEnabled(connected);
                finetuneButton.setEnabled(connected);
                for (Button btn : commandButtons) {
                    btn.setEnabled(connected);
                }
            }
        });
    }

    public class ConnectTask extends AsyncTask<String, String, TcpClient> {

        @Override
        protected TcpClient doInBackground(String... params) {
            final int tcpPort = Integer.parseInt(params[1]);
            final String userHost = params[0].trim();
            String endpoint = null;
            int resolvedPort = tcpPort;

            String cachedHost = loadCachedHost();
            if (!cachedHost.isEmpty() && !cachedHost.equalsIgnoreCase(userHost)) {
                publishProgress("Provo IP cached: " + cachedHost);
                if (TcpClient.canConnect(cachedHost, tcpPort, PROBE_CONNECT_TIMEOUT_MS)) {
                    endpoint = cachedHost;
                    publishProgress("Endpoint attivo da cache: " + endpoint);
                }
            }

            if (endpoint == null && !userHost.isEmpty()) {
                publishProgress("Provo host inserito: " + userHost);
                if (TcpClient.canConnect(userHost, tcpPort, PROBE_CONNECT_TIMEOUT_MS)) {
                    endpoint = userHost;
                    publishProgress("Endpoint attivo da input: " + endpoint);
                }
            }

            if (endpoint == null) {
                publishProgress("Discovery broadcast in corso...");
                DiscoveryClient.DiscoveryResult result = DiscoveryClient.discover();
                if (result != null && result.host != null && !result.host.isEmpty()) {
                    endpoint = result.host;
                    if (result.port > 0) {
                        resolvedPort = result.port;
                    }
                    publishProgress("Endpoint trovato via probe: " + endpoint + ":" + result.port);
                }
            }

            if (endpoint == null) {
                setConnectedStatus(false);
                publishProgress("Nessun endpoint raggiungibile (cache/input/probe).");
                return null;
            }

            final String resolvedEndpoint = endpoint;
            final int finalPort = resolvedPort;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hostnameEditText.setText(resolvedEndpoint);
                }
            });

            mTcpClient = new TcpClient(
                    new TcpClient.OnConnection() {
                        @Override
                        public void connected(String message) {
                            saveCachedHost(resolvedEndpoint);
                            App.getInstance().setTcpClient(mTcpClient);
                            setConnectedStatus(true);
                            publishProgress("Connesso a " + resolvedEndpoint + ":" + finalPort);
                        }

                        @Override
                        public void connectionFailed(String message) {
                            setConnectedStatus(false);
                            publishProgress("Connessione fallita su " + resolvedEndpoint + ":" + finalPort);
                        }
                    },
                    new TcpClient.OnMessageReceived() {
                        @Override
                        public void messageReceived(String message) {
                            publishProgress(message);
                            // Also forward to the App-level listener (for FineTuneActivity)
                            TcpClient.OnMessageReceived appListener = App.getInstance().getMessageListener();
                            if (appListener != null) {
                                appListener.messageReceived(message);
                            }
                        }
                    }
            );
            mTcpClient.run(resolvedEndpoint, String.valueOf(finalPort));
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            appendOutput(values[0]);
        }
    }

    private String loadCachedHost() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_LAST_HOST, "").trim();
    }

    private void saveCachedHost(String host) {
        if (host == null || host.trim().isEmpty()) return;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_LAST_HOST, host.trim()).apply();
    }
}
