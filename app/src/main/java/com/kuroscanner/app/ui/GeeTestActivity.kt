package com.kuroscanner.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.kuroscanner.app.R

class GeeTestActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geetest)

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {}

        webView.addJavascriptInterface(object : Any() {
            @android.webkit.JavascriptInterface
            fun onGeeTestSuccess(result: String) {
                val intent = Intent()
                intent.putExtra("geeTestData", result)
                intent.putExtra("phoneNumber", intent.getStringExtra("phoneNumber"))
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        })

        val geetestHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <script src="https://static.geetest.com/static/tools/gt.js"></script>
            </head>
            <body>
                <div id="captcha"></div>
                <script>
                    var handler = function (captchaObj) {
                        captchaObj.appendTo("#captcha");
                        captchaObj.onSuccess(function () {
                            var validate = captchaObj.getValidate();
                            window.android.onGeeTestSuccess(JSON.stringify(validate));
                        });
                    };
                    $.ajax({
                        url: "https://api.kurobbs.com/user/getSmsCode",
                        type: "get",
                        dataType: "json",
                        success: function (data) {
                            initGeetest({
                                gt: data.gt,
                                challenge: data.challenge,
                                new_captcha: data.new_captcha,
                                product: "popup"
                            }, handler);
                        }
                    });
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://api.kurobbs.com", geetestHtml, "text/html", "UTF-8", null)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}