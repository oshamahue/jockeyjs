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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.jockeyjs.util.ForwardingWebViewClient;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@SuppressWarnings({"HardCodedStringLiteral", "WeakerAccess"})
@SuppressLint("SetJavaScriptEnabled")
class JockeyWebViewClient extends ForwardingWebViewClient {

    private JockeyImpl _jockeyImpl;
    private WebViewClient _delegate;
    private Moshi moshi;

    public JockeyWebViewClient(@NonNull JockeyImpl jockey) {
        moshi = new Moshi.Builder().build();
        _jockeyImpl = jockey;
    }

    @NonNull
    public JockeyImpl getImplementation() {
        return _jockeyImpl;
    }

    protected void setDelegate(@Nullable WebViewClient client) {
        _delegate = client;
    }

    @Nullable
    public WebViewClient delegate() {
        return _delegate;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView webView, String callbackUrl) {

        if (delegate() != null
                && delegate().shouldOverrideUrlLoading(webView, callbackUrl))
            return true;

        try {
            URI uri = new URI(callbackUrl);

            if (isJockeyScheme(uri)) {
                processUri(webView, uri);
                return true;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (HostValidationException e) {
            e.printStackTrace();
            Log.e("Jockey", "The source of the event could not be validated!");
        }
        return false;
    }

    public boolean isJockeyScheme(@NonNull URI uri) {
        return uri.getScheme().equals("jockey") && !uri.getQuery().equals("");
    }

    public void processUri(@NonNull WebView view, @NonNull URI uri)
            throws HostValidationException {
        String[] parts = uri.getPath().replaceAll("^/", "").split("/");
        String host = uri.getHost();
        JsonAdapter<JockeyWebViewPayload> adapter = moshi.adapter(JockeyWebViewPayload.class);

        JockeyWebViewPayload payload = null;
        try {
            payload = checkPayload(adapter.fromJson(uri.getQuery()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (parts.length > 0) {
            if (host.equals("event")) {
                getImplementation().triggerEventFromWebView(view, payload);
            } else if (host.equals("callback")) {
                getImplementation().triggerCallbackForMessage(
                        Integer.parseInt(parts[0]));
            }
        }
    }

    public JockeyWebViewPayload checkPayload(@NonNull JockeyWebViewPayload fromJson)
            throws HostValidationException {
        validateHost(fromJson.host);
        return fromJson;
    }

    private void validateHost(@NonNull String host) throws HostValidationException {
        getImplementation().validate(host);
    }

}