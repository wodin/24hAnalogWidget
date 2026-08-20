package info.staticfree.android.twentyfourhour.overlay;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import java.time.LocalTime;
import java.util.Calendar;

import info.staticfree.android.twentyfourhour.dial.Dial;

public class HandsOverlay implements DialOverlay {
    private final Drawable hourHand;
    private final Drawable minuteHand;

    public HandsOverlay(@NonNull Drawable hourHand, @NonNull Drawable minuteHand) {
        this.hourHand = hourHand;
        this.minuteHand = minuteHand;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, int cX, int cY, int width, int height,
            @NonNull Calendar calendar, boolean sizeChanged) {

        int minute = calendar.get(Calendar.MINUTE);

        float minuteAngle = (minute / 60.0f) * 360;
        float hourAngle =
                Dial.angleFor(LocalTime.of(calendar.get(Calendar.HOUR_OF_DAY), minute));

        drawHand(canvas, minuteHand, minuteAngle, cX, cY, sizeChanged);
        drawHand(canvas, hourHand, hourAngle, cX, cY, sizeChanged);
    }

    private void drawHand(@NonNull Canvas canvas, @NonNull Drawable hand, float angle, int cX,
            int cY, boolean sizeChanged) {
        canvas.save();
        canvas.rotate(angle, cX, cY);

        if (sizeChanged) {
            setDrawableBounds(hand, cX, cY);
        }
        hand.draw(canvas);
        canvas.restore();
    }

    private void setDrawableBounds(@NonNull Drawable drawable, int cX, int cY) {
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        drawable.setBounds(cX - (w / 2), cY - (h / 2), cX + (w / 2), cY + (h / 2));
    }
}
