package info.staticfree.android.twentyfourhour;

/*
 * Copyright (C) 2026 Michael Wood
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */

/**
 * The one name every diagnosis in this app is written under.
 *
 * <p><strong>A deliberately shallow module, and the shallowest kind there is:
 * one constant.</strong> It carries no behaviour and hides nothing, which
 * normally condemns a class. It exists because Java has nowhere else to put a
 * constant that belongs to the whole app — there is no package-level constant
 * and no file-level one — so the choice was never between this class and
 * something deeper, only between this class and the same string typed out at
 * every site. It is not an abstraction and should not grow into one; the day it
 * has a second member is the day to ask what it is really for.
 *
 * <p><strong>Why one tag beats a tag per class.</strong> Every site here used to
 * name itself, {@code getSimpleName()}-style, which reads well at the call site
 * and fails the only reader it has. A tag is not a label on a line, it is the
 * argument to {@code adb logcat -s} — a filter that must be written
 * <em>before</em> the line appears, by somebody who does not yet know which
 * class gave up. Per-class tags ask that person to guess the name of the file
 * they are trying to find, and to have guessed all of them, since the filter
 * takes a fixed list. So the README could publish one class's name and did, and
 * every diagnosis outside that class was invisible to anyone following it.
 *
 * <p>The tag is short for one reason and one only: it is typed by hand at a
 * prompt. Not for the platform's old 23-character tag limit, which
 * {@code Log.isLoggable} enforced below API 24 and this app's {@code minSdk 26}
 * puts out of reach — a constraint that cannot bind is not a reason.
 *
 * <p><strong>This is a published interface.</strong> The string is meant to be
 * read by a person at a prompt rather than by any code, so it is documentation
 * that happens to compile, and the README's {@code adb logcat -s} is where it
 * is handed out. Which makes the literal load-bearing in a way a constant
 * usually is not: nothing links the README to this file, so renaming the
 * constant would leave a published command matching nothing — exactly the
 * silence the diagnosis exists to break. {@code DiagnosisTest} pins the literal
 * for that reason, and only that reason.
 */
public final class Diagnosis {

    /**
     * The tag. Every {@code Log} call in this app passes this and nothing else.
     */
    public static final String TAG = "24hWidget";

    private Diagnosis() {
    }
}
