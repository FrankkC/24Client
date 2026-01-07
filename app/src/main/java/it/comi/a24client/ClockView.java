package it.comi.a24client;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

public class ClockView extends View {

    private float hand1Angle = 0;
    private float hand2Angle = 0;

    private Paint paintCircle;
    private Paint paintHand1;
    private Paint paintHand2;

    public ClockView(Context context) {
        super(context);
        init();
    }

    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintCircle = new Paint();
        paintCircle.setColor(Color.LTGRAY);
        paintCircle.setStyle(Paint.Style.STROKE);
        paintCircle.setStrokeWidth(5);
        paintCircle.setAntiAlias(true);

        paintHand1 = new Paint();
        paintHand1.setColor(Color.RED); // Hours?
        paintHand1.setStrokeWidth(8);
        paintHand1.setAntiAlias(true);

        paintHand2 = new Paint();
        paintHand2.setColor(Color.BLUE); // Minutes?
        paintHand2.setStrokeWidth(8);
        paintHand2.setAntiAlias(true);
    }

    public void setHand1Angle(float angle) {
        this.hand1Angle = angle;
        invalidate();
    }

    public void setHand2Angle(float angle) {
        this.hand2Angle = angle;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width, height) / 2 - 10;
        int cx = width / 2;
        int cy = height / 2;

        // Draw clock face
        canvas.drawCircle(cx, cy, radius, paintCircle);

        // Save layer to composite hands with blending
        int sc = canvas.saveLayer(0, 0, width, height, null);

        // Draw Hand 1 (Red)
        drawHand(canvas, cx, cy, radius * 0.8f, hand1Angle, paintHand1);

        // Draw Hand 2 (Blue) with LIGHTEN mode to mix colors
        paintHand2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
        drawHand(canvas, cx, cy, radius * 0.8f, hand2Angle, paintHand2);
        paintHand2.setXfermode(null);

        // Restore layer
        canvas.restoreToCount(sc);
    }

    private void drawHand(Canvas canvas, int cx, int cy, float length, float angleDegrees, Paint paint) {
        // Angle 0 is usually 3 o'clock in math, but for clocks 0 is 12 o'clock.
        // If the server sends 0 for 12 o'clock, we need to adjust.
        // Assuming 0 is 12 o'clock (up), and clockwise.
        // Math: 0 is right (3 o'clock), counter-clockwise.
        // So: mathAngle = -clockAngle + 90?
        // Let's assume the server sends degrees where 0 is 12 o'clock and increases clockwise.
        // If 0 is 12 o'clock:
        // x = cx + length * sin(rad)
        // y = cy - length * cos(rad)
        
        double rad = Math.toRadians(angleDegrees);
        float endX = (float) (cx + length * Math.sin(rad));
        float endY = (float) (cy - length * Math.cos(rad));

        canvas.drawLine(cx, cy, endX, endY, paint);
    }
}
