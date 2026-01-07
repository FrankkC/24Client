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
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;

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
    EditText dataEditText;

    // Grid constants
    private static final int GRID_ROWS = 6;
    private static final int GRID_COLS = 4;

    // Grid selection state
    private int selectedRow = 0;
    private int selectedCol = 0;
    private int selectedHand = 0; // 0: Hours, 1: Minutes
    private int selectedDegrees = 0;
    private ClockView[][] clockViews = new ClockView[GRID_ROWS][GRID_COLS];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        TextView textView;

        textView = new TextView(this);
        textView.setText("hostname / IP:");
        linearLayout.addView(textView);

        LinearLayout ipContainer = new LinearLayout(this);
        ipContainer.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.addView(ipContainer);

        final EditText hostnameEditText = new EditText(this);
        hostnameEditText.setText("10.10.0.22");
        hostnameEditText.setSingleLine(true);
        LinearLayout.LayoutParams ipParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        hostnameEditText.setLayoutParams(ipParams);
        ipContainer.addView(hostnameEditText);

        connectButton = new Button(this);
        connectButton.setText("CONNECT");
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTcpClient != null && mTcpClient.isConnected()) {
                    mTcpClient.stopClient();
                    mTcpClient = null;
                    setConnectedStatus(false);
                } else {
                    new ConnectTask().execute(hostnameEditText.getText().toString(), "23");
                }
            }
        });
        ipContainer.addView(connectButton);

        textView = new TextView(this);
        textView.setText("data to send:");
        linearLayout.addView(textView);

        LinearLayout dataContainer = new LinearLayout(this);
        dataContainer.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.addView(dataContainer);

        dataEditText = new EditText(this);
        dataEditText.setText("ECHO");
        LinearLayout.LayoutParams dataParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        dataEditText.setLayoutParams(dataParams);
        dataContainer.addView(dataEditText);

        sendButton = new Button(this);
        sendButton.setText("SEND");
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTcpClient != null) {
                    String dataToSend = dataEditText.getText().toString();
                    mTcpClient.sendMessage(dataToSend);
                }
            }
        });
        sendButton.setEnabled(false);
        dataContainer.addView(sendButton);

        // --- Grid UI ---
        TextView gridLabel = new TextView(this);
        gridLabel.setText("Select Clock (Tap to toggle Hand):");
        linearLayout.addView(gridLabel);

        TextView legendLabel = new TextView(this);
        legendLabel.setText("Red: Hand 1 (Even Motor), Blue: Hand 2 (Odd Motor)");
        linearLayout.addView(legendLabel);

        LinearLayout gridContainer = new LinearLayout(this);
        gridContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        linearLayout.addView(gridContainer, gridParams);

        for (int r = 0; r < GRID_ROWS; r++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setWeightSum(GRID_COLS);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1.0f
            );
            gridContainer.addView(rowLayout, rowParams);

            for (int c = 0; c < GRID_COLS; c++) {
                final int row = r;
                final int col = c;
                ClockView cv = new ClockView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1.0f
                );
                cv.setLayoutParams(params);
                cv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        handleClockClick(row, col);
                    }
                });
                clockViews[r][c] = cv;
                rowLayout.addView(cv);
            }
        }

        // --- Slider UI ---
        final TextView sliderLabel = new TextView(this);
        sliderLabel.setText("Rotate: 0");
        linearLayout.addView(sliderLabel);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(180); // Range -90 to +90
        seekBar.setProgress(90); // 0
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedDegrees = progress - 90;
                sliderLabel.setText("Rotate: " + selectedDegrees);
                updateDataString();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        linearLayout.addView(seekBar);

        // Initialize
        updateGridSelection();
        updateDataString();

        replyTextView = new TextView(this);
        final ScrollView replyTextScrollView = new ScrollView(this);
        replyTextScrollView.addView(replyTextView);

        final TextView outputLabel = new TextView(this);
        outputLabel.setText("output:");
        outputLabel.setVisibility(View.GONE);

        final Button toggleOutputButton = new Button(this);
        toggleOutputButton.setText("Show Output");
        toggleOutputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (replyTextScrollView.getVisibility() == View.VISIBLE) {
                    replyTextScrollView.setVisibility(View.GONE);
                    outputLabel.setVisibility(View.GONE);
                    toggleOutputButton.setText("Show Output");
                } else {
                    replyTextScrollView.setVisibility(View.VISIBLE);
                    outputLabel.setVisibility(View.VISIBLE);
                    toggleOutputButton.setText("Hide Output");
                }
            }
        });
        linearLayout.addView(toggleOutputButton);
        linearLayout.addView(outputLabel);
        replyTextScrollView.setVisibility(View.GONE);
        linearLayout.addView(replyTextScrollView);

        this.setContentView(linearLayout);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mTcpClient != null) {
            mTcpClient.stopClient();
        }
    }

    private void handleClockClick(int row, int col) {
        if (selectedRow == row && selectedCol == col) {
            // Toggle hand
            selectedHand = 1 - selectedHand; // Toggle 0 <-> 1
        } else {
            // Select new clock, reset hand to Hours (0)
            selectedRow = row;
            selectedCol = col;
            selectedHand = 0;
        }
        updateGridSelection();
        updateDataString();
    }

    private void updateGridSelection() {
        for (int r = 0; r < GRID_ROWS; r++) {
            for (int c = 0; c < GRID_COLS; c++) {
                ClockView cv = clockViews[r][c];
                if (cv == null) continue;
                if (r == selectedRow && c == selectedCol) {
                    cv.setBackgroundColor(Color.CYAN);
                } else {
                    cv.setBackgroundColor(Color.LTGRAY);
                }
            }
        }
    }

    private void updateDataString() {
        if (dataEditText != null) {
            char handChar = (selectedHand == 0) ? '0' : '1';
            String data = String.format("%d%d%c%+d", selectedRow, selectedCol, handChar, selectedDegrees);
            dataEditText.setText(data);
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
            replyTextView.append(values[0] + "\n");
            parseMessage(values[0]);
        }

    }

    private void parseMessage(String message) {
        if (message == null) return;

        // Check if it is a position message
        if (message.contains("POS=")) {
            try {
                int slaveId = -1;
                if (message.contains("[SLAVE 1]")) {
                    slaveId = 1;
                } else if (message.contains("[SLAVE 2]")) {
                    slaveId = 2;
                }

                if (slaveId != -1) {
                    int posIndex = message.indexOf("POS=");
                    String valuesStr = message.substring(posIndex + 4).trim();
                    String[] values = valuesStr.split(",");

                    if (values.length == 24) {
                        updateClocks(slaveId, values);
                    }
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error parsing message", e);
            }
        }
    }

    private void updateClocks(int slaveId, String[] values) {
        // Slave 1 manages Col 0 and Col 1
        // Slave 2 manages Col 2 and Col 3
        int colLeft = (slaveId == 1) ? 0 : 2;
        int colRight = (slaveId == 1) ? 1 : 3;

        // The server sends data board by board.
        // Each slave handles 6 boards and each board controls one row (2 clocks).
        // Board i controls Row i.
        // Board i motors 0,1 -> Left Clock (colLeft)
        // Board i motors 2,3 -> Right Clock (colRight)

        for (int row = 0; row < GRID_ROWS; row++) {
            int baseIndex = row * 4;
            
            if (baseIndex + 3 >= values.length) break;

            try {
                // Left Clock (Motors 0, 1 of the board)
                float valL1 = Float.parseFloat(values[baseIndex]);
                float valL2 = Float.parseFloat(values[baseIndex + 1]);
                if (clockViews[row][colLeft] != null) {
                    clockViews[row][colLeft].setHand1Angle(valL1);
                    clockViews[row][colLeft].setHand2Angle(valL2);
                }

                // Right Clock (Motors 2, 3 of the board)
                float valR1 = Float.parseFloat(values[baseIndex + 2]);
                float valR2 = Float.parseFloat(values[baseIndex + 3]);
                if (clockViews[row][colRight] != null) {
                    clockViews[row][colRight].setHand1Angle(valR1);
                    clockViews[row][colRight].setHand2Angle(valR2);
                }

            } catch (NumberFormatException e) {
                Log.e("MainActivity", "Error parsing float", e);
            }
        }
    }
}