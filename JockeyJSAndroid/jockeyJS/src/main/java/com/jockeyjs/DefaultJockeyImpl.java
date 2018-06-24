package com.jockeyjs;

import android.webkit.WebView;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;


@SuppressWarnings("HardCodedStringLiteral")
public class DefaultJockeyImpl extends JockeyImpl {

    private int messageCount = 0;
    Moshi moshi = new Moshi.Builder().build();

    @Override
    public void send(String type, WebView toWebView, Object withPayload,
                     JockeyCallback complete) {
        int messageId = messageCount;

        if (complete != null) {
            add(messageId, complete);
        }

        if (withPayload != null) {
            JsonAdapter<Object> jsonAdapter = moshi.adapter(Object.class);
            withPayload = jsonAdapter.toJson(withPayload);
        }

        String url = String.format("javascript:Jockey.trigger(\"%s\", %d, %s)",
                type, messageId, withPayload);
        toWebView.loadUrl(url);

        ++messageCount;
    }

    @Override
    public void triggerCallbackOnWebView(WebView webView, int messageId) {
        String url = String.format("javascript:Jockey.triggerCallback(\"%d\")",
                messageId);
        webView.loadUrl(url);
    }

}
