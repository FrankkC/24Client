package it.comi.a24client;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class FineTuneActivity extends Activity {

    private static final int GRID_ROWS = 6;
    private static final int GRID_COLS = 4;

    private ClockView[][] clockViews = new ClockView[GRID_ROWS][GRID_COLS];
    private int selectedRow = 0;
    private int selectedCol = 0;
    private int selectedHand = 0; // 0 = Hours, 1 = Minutes
    private int selectedDegrees = 0;

    private TextView selectionLabel;
    private TextView sliderLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);

        // === Title bar with close button ===
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(titleRow);

        TextView title = new TextView(this);
        title.setText("FINETUNE");
        title.setTextSize(18);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        titleRow.addView(title);

        Button closeButton = new Button(this);
        closeButton.setText("✕ Chiudi");
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        titleRow.addView(closeButton);

        // === Clock Grid ===
        LinearLayout gridContainer = new LinearLayout(this);
        gridContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        root.addView(gridContainer, gridParams);

        for (int r = 0; r < GRID_ROWS; r++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setWeightSum(GRID_COLS);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
            gridContainer.addView(rowLayout, rowParams);

            for (int c = 0; c < GRID_COLS; c++) {
                final int row = r;
                final int col = c;
                ClockView cv = new ClockView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
                cv.setLayoutParams(params);
                cv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleClockClick(row, col);
                    }
                });
                clockViews[r][c] = cv;
                rowLayout.addView(cv);
            }
        }

        // === Selection label ===
        selectionLabel = new TextView(this);
        selectionLabel.setPadding(0, 8, 0, 4);
        root.addView(selectionLabel);

        // === Legend ===
        TextView legend = new TextView(this);
        legend.setText("Rosso: Ore — Blu: Minuti — Tap per cambiare lancetta");
        legend.setTextSize(11);
        legend.setTextColor(Color.GRAY);
        root.addView(legend);

        // === Slider ===
        sliderLabel = new TextView(this);
        sliderLabel.setPadding(0, 8, 0, 0);
        root.addView(sliderLabel);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(180); // -90 to +90
        seekBar.setProgress(90); // center = 0
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedDegrees = progress - 90;
                updateLabels();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(seekBar);

        // === SET button ===
        Button setButton = new Button(this);
        setButton.setText("SET");
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendFineTune();
            }
        });
        root.addView(setButton);

        setContentView(root);

        updateGridSelection();
        updateLabels();

        // Auto-request positions on open
        requestPositions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register as message listener to receive POS updates
        App.getInstance().setMessageListener(new TcpClient.OnMessageReceived() {
            @Override
            public void messageReceived(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        parseMessage(message);
                    }
                });
            }
        });
    }

    private void handleClockClick(int row, int col) {
        if (selectedRow == row && selectedCol == col) {
            selectedHand = 1 - selectedHand;
        } else {
            selectedRow = row;
            selectedCol = col;
            selectedHand = 0;
        }
        updateGridSelection();
        updateLabels();
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

    private void updateLabels() {
        String handName = (selectedHand == 0) ? "Ore" : "Minuti";
        selectionLabel.setText(String.format("Selezionato: R%d C%d — Lancetta: %s", selectedRow, selectedCol, handName));
        sliderLabel.setText(String.format("Regolazione: %+d°", selectedDegrees));
    }

    private void sendFineTune() {
        TcpClient client = App.getInstance().getTcpClient();
        if (client != null) {
            String cmd = String.format("FINETUNE=%d,%d,%d,%+.2f",
                    selectedRow, selectedCol, selectedHand, (float) selectedDegrees);
            client.sendMessage(cmd);
        }
    }

    private void requestPositions() {
        TcpClient client = App.getInstance().getTcpClient();
        if (client != null) {
            client.sendMessage("GETPOS");
        }
    }

    private void parseMessage(String message) {
        if (message == null) return;

        if (message.startsWith("POS ")) {
            try {
                int slaveId = Character.getNumericValue(message.charAt(4));
                String valuesStr = message.substring(6).trim();
                String[] values = valuesStr.split(",");

                if (values.length == 24 && (slaveId == 1 || slaveId == 2)) {
                    updateClocks(slaveId, values);
                }
            } catch (Exception e) {
                Log.e("FineTuneActivity", "Error parsing POS message", e);
            }
        }
    }

    private void updateClocks(int slaveId, String[] values) {
        int colLeft = (slaveId == 1) ? 0 : 2;
        int colRight = (slaveId == 1) ? 1 : 3;

        int[] boardToRow = {0, 1, 2, 3, 4, 5};

        for (int boardIndex = 0; boardIndex < 6; boardIndex++) {
            int row = boardToRow[boardIndex];
            int baseIndex = boardIndex * 4;

            if (baseIndex + 3 >= values.length) break;

            try {
                float valL1 = Float.parseFloat(values[baseIndex]);
                float valL2 = Float.parseFloat(values[baseIndex + 1]);
                if (clockViews[row][colLeft] != null) {
                    clockViews[row][colLeft].setHand1Angle(valL1);
                    clockViews[row][colLeft].setHand2Angle(valL2);
                }

                float valR1 = Float.parseFloat(values[baseIndex + 2]);
                float valR2 = Float.parseFloat(values[baseIndex + 3]);
                if (clockViews[row][colRight] != null) {
                    clockViews[row][colRight].setHand1Angle(valR1);
                    clockViews[row][colRight].setHand2Angle(valR2);
                }
            } catch (NumberFormatException e) {
                Log.e("FineTuneActivity", "Error parsing float", e);
            }
        }
    }
}
