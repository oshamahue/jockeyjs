/*******************************************************************************
 * Copyright (c) 2013,  Paul Daniels
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package com.jockeyjs;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.jockeyjs.JockeyHandler.OnCompletedListener;
import com.jockeyjs.util.ForwardingWebViewClient;

import java.util.HashMap;
import java.util.Map;

public abstract class JockeyImpl implements Jockey {

    // A default Callback that does nothing.
    @NonNull
    protected static final JockeyCallback _DEFAULT = new JockeyCallback() {
        @Override
        public void call() {
        }
    };

    private Map<String, CompositeJockeyHandler> _listeners = new HashMap<>();
    private SparseArray<JockeyCallback> _callbacks = new SparseArray<>();

    @Nullable
    private OnValidateListener _onValidateListener;

    private Handler _handler = new Handler();

    private JockeyWebViewClient _client;

    public JockeyImpl() {
        _client = new JockeyWebViewClient(this);
    }


    @NonNull
    public static Jockey getDefault() {
        return new DefaultJockeyImpl();
    }

    @Override
    public void send(@NonNull String type, @NonNull WebView toWebView) {
        send(type, toWebView, null);
    }

    @Override
    public void send(@NonNull String type, @NonNull WebView toWebView, @Nullable Object withPayload) {
        send(type, toWebView, withPayload, null);
    }

    @Override
    public void send(@NonNull String type, @NonNull WebView toWebView, @Nullable JockeyCallback complete) {
        send(type, toWebView, null, complete);

    }

    @Override
    public void on(@NonNull String type, @NonNull JockeyHandler... handler) {

        if (!this.handles(type)) {
            _listeners.put(type, new CompositeJockeyHandler());
        }

        _listeners.get(type).add(handler);
    }

    @Override
    public void off(@NonNull String type) {
        _listeners.remove(type);
    }

    @Override
    public boolean handles(@NonNull String eventName) {
        return _listeners.containsKey(eventName);
    }

    protected void add(int messageId, @NonNull JockeyCallback callback) {
        _callbacks.put(messageId, callback);
    }

    protected void triggerEventFromWebView(@NonNull final WebView webView,
                                           @Nullable JockeyWebViewPayload envelope) {
        final int messageId = envelope.id;
        String type = envelope.type;

        if (this.handles(type)) {
            JockeyHandler handler = _listeners.get(type);

            handler.perform(envelope.payload, new OnCompletedListener() {
                @Override
                public void onCompleted() {
                    // This has to be done with a handler because a webview load
                    // must be triggered
                    // in the UI thread
                    _handler.post(new Runnable() {
                        @Override
                        public void run() {
                            triggerCallbackOnWebView(webView, messageId);
                        }
                    });
                }
            });
        }
    }

    protected void triggerCallbackForMessage(@NonNull int messageId) {
        try {
            JockeyCallback complete = _callbacks.get(messageId, _DEFAULT);
            complete.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        _callbacks.remove(messageId);
    }

    public void validate(@NonNull String host) throws HostValidationException {
        if (_onValidateListener != null && !_onValidateListener.validate(host)) {
            throw new HostValidationException();
        }
    }

    @Override
    public void setOnValidateListener(@Nullable OnValidateListener listener) {
        _onValidateListener = listener;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void configure(@NonNull WebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(this.getWebViewClient());
    }

    @NonNull
    protected ForwardingWebViewClient getWebViewClient() {
        return this._client;
    }

    @Override
    public void setWebViewClient(@NonNull WebViewClient client) {
        this._client.setDelegate(client);
    }

}
