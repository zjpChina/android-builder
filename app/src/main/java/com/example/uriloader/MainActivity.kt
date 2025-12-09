package com.example.uriloader

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        setupWebView()
        loadConfiguredUri()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Toast.makeText(this@MainActivity, "加载失败: $description", Toast.LENGTH_SHORT).show()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun loadConfiguredUri() {
        val uri = getConfiguredUri()
        if (uri.isNotEmpty()) {
            progressBar.visibility = View.VISIBLE
            webView.loadUrl(uri)
        } else {
            Toast.makeText(this, "未配置有效的URI", Toast.LENGTH_LONG).show()
            // 显示默认页面
            webView.loadData(
                """
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                            margin: 0;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            text-align: center;
                        }
                        .container {
                            padding: 20px;
                        }
                        h1 { font-size: 24px; margin-bottom: 16px; }
                        p { font-size: 16px; opacity: 0.9; }
                        code {
                            background: rgba(255,255,255,0.2);
                            padding: 4px 8px;
                            border-radius: 4px;
                            font-size: 14px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🔗 URI Loader</h1>
                        <p>请配置 <code>config.json</code> 文件中的 URI</p>
                        <p>配置文件位置：<br><code>/sdcard/Android/data/com.example.uriloader/files/config.json</code></p>
                    </div>
                </body>
                </html>
                """.trimIndent(),
                "text/html",
                "UTF-8"
            )
        }
    }

    private fun getConfiguredUri(): String {
        // 优先读取外部存储的配置文件
        val externalConfigFile = File(getExternalFilesDir(null), "config.json")
        if (externalConfigFile.exists()) {
            try {
                val jsonString = externalConfigFile.readText()
                val jsonObject = JSONObject(jsonString)
                return jsonObject.optString("uri", "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 回退到assets中的默认配置
        try {
            val inputStream = assets.open("config.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            reader.close()
            
            val jsonObject = JSONObject(jsonString)
            return jsonObject.optString("uri", "")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ""
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
