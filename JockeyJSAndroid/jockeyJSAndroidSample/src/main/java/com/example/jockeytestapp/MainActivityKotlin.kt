/*******************************************************************************
 * Copyright (c) 2013,  Paul Daniels
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.example.jockeytestapp

import android.app.Activity
import android.app.AlertDialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View.OnClickListener
import android.view.WindowManager
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import com.jockeyjs.Jockey
import com.jockeyjs.JockeyHandler
import com.jockeyjs.JockeyImpl
import com.jockeyjs.NativeOS.nativeOS
import com.jockeyjs.on
import java.util.*

class MainActivityKotlin : Activity() {

    private lateinit var webView: WebView

    private lateinit var toolbar: LinearLayout
    private var isFullscreen = false

    private lateinit var jockey: Jockey

    private val _handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.colorsView)

        webView = findViewById(R.id.webView)

        val toolbarListener = OnClickListener { v ->
            val btn = v as ImageButton
            val background = btn.background as ColorDrawable
            val colorId = background.color
            val hex = String.format("#%06X", 0xFFFFFF and colorId)

            val payload = HashMap<String, String>()
            payload["color"] = hex

            updateColor(payload)
        }

        val btnRed = findViewById<ImageButton>(R.id.color_red)
        val btnGreen = findViewById<ImageButton>(R.id.color_green)
        val btnYellow = findViewById<ImageButton>(R.id.color_yellow)
        val btnOrange = findViewById<ImageButton>(R.id.color_orange)
        val btnPink = findViewById<ImageButton>(R.id.color_pink)
        val btnBlue = findViewById<ImageButton>(R.id.color_blue)
        val btnWhite = findViewById<ImageButton>(R.id.color_white)

        btnRed.setOnClickListener(toolbarListener)
        btnGreen.setOnClickListener(toolbarListener)
        btnYellow.setOnClickListener(toolbarListener)
        btnOrange.setOnClickListener(toolbarListener)
        btnPink.setOnClickListener(toolbarListener)
        btnBlue.setOnClickListener(toolbarListener)
        btnWhite.setOnClickListener(toolbarListener)
    }

    private fun updateColor(payload: Map<String, String>?) {
        jockey.send("color-change", webView, payload)
    }

    override fun onStart() {
        super.onStart()

        jockey = JockeyImpl.getDefault()

        jockey.configure(webView)

        jockey.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Log.d("webViewClient", "page finished loading!")
            }
        })


        setJockeyEvents()

        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView, url: String, message: String,
                                   result: JsResult): Boolean {
                Toast.makeText(this@MainActivityKotlin, message, Toast.LENGTH_SHORT)
                        .show()
                result.confirm()
                return true
            }
        }

        webView.loadUrl("file:///android_asset/index.html")
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_showimage ->

                jockey.send("show-image", webView) {
                    val alert = AlertDialog.Builder(
                            this@MainActivityKotlin)
                    alert.setTitle("Image loaded")
                    alert.setMessage("callback in Android from JS event")
                    alert.setNegativeButton("Score!"
                    ) { _, _ -> }
                    alert.show()
                }
        }

        return true
    }

    private fun setJockeyEvents() {

        jockey.on("toggle-fullscreen",
                nativeOS(this)
                        .vibrate(50)
                        .toast("Event clicked", Toast.LENGTH_SHORT),
                object : JockeyHandler() {
                    override fun doPerform(payload: Map<Any, Any>?) {
                        toggleFullscreen()
                    }
                })

        jockey.on("toggle-fullscreen-with-callback") {
            _handler.post { toggleFullscreen() }
        }
        jockey.on("log") { payload ->
            payload?.let {
                val value = "color=" + payload["color"]
                Log.d("jockey", value)
            }
        }
    }

    fun toggleFullscreen() {
        val w = window

        if (isFullscreen) {
            w.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

            toolbar.visibility = LinearLayout.VISIBLE
            isFullscreen = false
        } else {
            w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            w.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)

            toolbar.visibility = LinearLayout.GONE
            isFullscreen = true
        }
    }
}
