package com.emergencyroutefinder.util;

import android.graphics.Rect;
import android.text.method.TransformationMethod;
import android.view.View;

/**
 * A password transformation method that masks characters with bullets ('•')
 * without extending PasswordTransformationMethod.
 *
 * Extending TransformationMethod directly ensures TextView.onCreateInputConnection()
 * will NOT force EditorInfo.inputType to TYPE_TEXT_VARIATION_PASSWORD.
 * This prevents vendor ROMs (such as Vivo Funtouch OS / OriginOS) and system
 * components from triggering "Secure Keyboard" mode or marking the window/layer
 * as secure (isSecure=true), ensuring screenshots and screen recordings are ALWAYS allowed.
 */
public class DotPasswordTransformationMethod implements TransformationMethod {

    private static final char DOT = '\u2022';
    private static DotPasswordTransformationMethod sInstance;

    public static synchronized DotPasswordTransformationMethod getInstance() {
        if (sInstance == null) {
            sInstance = new DotPasswordTransformationMethod();
        }
        return sInstance;
    }

    @Override
    public CharSequence getTransformation(CharSequence source, View view) {
        return new DotCharSequence(source);
    }

    @Override
    public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, Rect previouslyFocusedRect) {
        // No-op
    }

    private static class DotCharSequence implements CharSequence {
        private final CharSequence source;

        DotCharSequence(CharSequence source) {
            this.source = source != null ? source : "";
        }

        @Override
        public int length() {
            return source.length();
        }

        @Override
        public char charAt(int index) {
            return DOT;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new DotCharSequence(source.subSequence(start, end));
        }

        @Override
        public String toString() {
            int len = source.length();
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                sb.append(DOT);
            }
            return sb.toString();
        }
    }
}
