package com.jockeyjs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.WebView;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.Locale;


@SuppressWarnings("HardCodedStringLiteral")
public class DefaultJockeyImpl extends JockeyImpl {

    private int messageCount = 0;
    Moshi moshi = new Moshi.Builder().build();

    @Override
    public void send(@NonNull String type, @NonNull WebView toWebView, @Nullable Object withPayload,
                     @Nullable JockeyCallback complete) {
        int messageId = messageCount;

        if (complete != null) {
            add(messageId, complete);
        }

        if (withPayload != null) {
            JsonAdapter<Object> jsonAdapter = moshi.adapter(Object.class);
            withPayload = jsonAdapter.toJson(withPayload);
        }

        String url = String.format(Locale.getDefault(), "javascript:Jockey.trigger(\"%s\", %d, %s)",
                type, messageId, withPayload);
        toWebView.loadUrl(url);

        ++messageCount;
    }

    @Override
    public void triggerCallbackOnWebView(@NonNull WebView webView, int messageId) {
        String url = String.format(Locale.getDefault(), "javascript:Jockey.triggerCallback(\"%d\")",
                messageId);
        webView.loadUrl(url);
    }

}
