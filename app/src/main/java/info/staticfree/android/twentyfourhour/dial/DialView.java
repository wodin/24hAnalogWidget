package info.staticfree.android.twentyfourhour.dial;
/*
 * Copyright (C) 2011-2014 Steve Pomeroy <steve@staticfree.info>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Shows a {@link Dial} in a layout. Nothing more: the arrangement of the dial,
 * the scaling and the time all belong to the Dial, and this class only supplies
 * the bounds a View knows and the Dial does not.
 *
 * <p>Keeping this adapter is deliberate. The widget receiver is to become a
 * second host that draws a Dial straight to a bitmap with no View involved, and
 * two hosts is what keeps {@link Dial#draw} shaped like a dial rather than like
 * whichever single caller it happens to have.
 */
public class DialView extends View {

    @NonNull
    private Dial dial;

    /** The moment to show, or null to show the present one. */
    @Nullable
    private Instant time;

    /** Reused, because allocating in {@link #onDraw} is allocating every frame. */
    @NonNull
    private final Rect bounds = new Rect();

    public DialView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        dial = Dial.createStandard(context);
    }

    public DialView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DialView(@NonNull Context context) {
        this(context, null, 0);
    }

    /** Replaces the dial shown, which is how a caller chooses its overlays. */
    public void setDial(@NonNull Dial dial) {
        this.dial = dial;

        requestLayout();
        invalidate();
    }

    /**
     * Pins the displayed time. Without this the view shows the present moment
     * as of each draw, which is what a clock does; with it, whatever instant you
     * name, which is what a test or a preview wants.
     */
    public void setTime(@NonNull Instant time) {
        this.time = time;

        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        bounds.set(0, 0, getWidth(), getHeight());
        dial.draw(canvas, bounds, time != null ? time : Instant.now(), ZoneId.systemDefault());
    }

    // View.onMeasure already reads both of these through getDefaultSize, so
    // answering here sizes the view without an onMeasure of our own.
    @Override
    protected int getSuggestedMinimumWidth() {
        return dial.getIntrinsicSize();
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return dial.getIntrinsicSize();
    }
}
