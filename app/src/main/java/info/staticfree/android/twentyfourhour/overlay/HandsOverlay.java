package info.staticfree.android.twentyfourhour.overlay;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import info.staticfree.android.twentyfourhour.dial.Dial;

public class HandsOverlay implements DialOverlay {
    private static final float DEGREES_PER_TURN = 360;
    private static final float MINUTES_PER_HOUR = 60;

    private final Drawable hourHand;
    private final Drawable minuteHand;

    public HandsOverlay(@NonNull Drawable hourHand, @NonNull Drawable minuteHand) {
        this.hourHand = hourHand;
        this.minuteHand = minuteHand;
    }

    @Override
    public void draw(@NonNull Canvas canvas, int side, @NonNull Instant instant,
            @NonNull ZoneId zone) {
        // Truncated here, not left to whoever we hand the time to: there is no
        // second hand, so both hands step by the minute whatever resolution the
        // dial's own mapping later grows.
        LocalTime time = instant.atZone(zone).toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        int centre = side / 2;

        // The minute hand turns once an hour. The hour hand turns once a day,
        // which is the dial's identity rather than the hands', so the Dial maps
        // it.
        float minuteAngle = time.getMinute() / MINUTES_PER_HOUR * DEGREES_PER_TURN;

        drawHand(canvas, minuteHand, minuteAngle, centre);
        drawHand(canvas, hourHand, Dial.angleFor(time), centre);
    }

    private void drawHand(@NonNull Canvas canvas, @NonNull Drawable hand, float angle, int centre) {
        canvas.save();
        canvas.rotate(angle, centre, centre);
        setDrawableBounds(hand, centre);
        hand.draw(canvas);
        canvas.restore();
    }

    private void setDrawableBounds(@NonNull Drawable drawable, int centre) {
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        drawable.setBounds(centre - (w / 2), centre - (h / 2), centre + (w / 2), centre + (h / 2));
    }
}
