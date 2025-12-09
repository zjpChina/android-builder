package com.example.uriloader

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var sharedPreferences: SharedPreferences
    
    private val handler = Handler(Looper.getMainLooper())
    private var isLongPress = false
    private val longPressRunnable = Runnable {
        isLongPress = true
        showConfigDialog()
    }

    companion object {
        private const val PREF_NAME = "AppConfig"
        private const val KEY_SAVED_URI = "saved_uri"
        private const val LONG_PRESS_DURATION = 5000L // 5 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        setupWebView()
        loadConfiguredUri()
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
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

        // 添加长按检测
        webView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLongPress = false
                    handler.postDelayed(longPressRunnable, LONG_PRESS_DURATION)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPressRunnable)
                }
            }
            false // 不消费事件，让WebView正常处理点击和滑动
        }
    }

    private fun showConfigDialog() {
        val currentUri = getConfiguredUri()
        val input = EditText(this)
        input.setText(currentUri)
        input.setSelection(input.text.length)

        AlertDialog.Builder(this)
            .setTitle("配置 URI")
            .setMessage("请输入要加载的网址：")
            .setView(input)
            .setPositiveButton("保存并加载") { _, _ ->
                val newUri = input.text.toString().trim()
                if (newUri.isNotEmpty()) {
                    saveUri(newUri)
                    webView.loadUrl(newUri)
                    Toast.makeText(this, "已保存配置", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("清除配置") { _, _ ->
                clearSavedUri()
                loadConfiguredUri() // 重新加载默认配置
                Toast.makeText(this, "已清除自定义配置", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun saveUri(uri: String) {
        sharedPreferences.edit().putString(KEY_SAVED_URI, uri).apply()
    }

    private fun clearSavedUri() {
        sharedPreferences.edit().remove(KEY_SAVED_URI).apply()
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
                        <p>长按屏幕 5 秒可手动配置地址</p>
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
        // 1. 优先读取 SharedPreferences 中保存的配置
        val savedUri = sharedPreferences.getString(KEY_SAVED_URI, "")
        if (!savedUri.isNullOrEmpty()) {
            return savedUri
        }

        // 2. 读取外部存储的配置文件
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

        // 3. 回退到assets中的默认配置
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
