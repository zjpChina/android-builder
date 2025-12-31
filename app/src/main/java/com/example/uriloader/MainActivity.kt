package com.example.uriloader

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
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
    private var touchStartX = 0f
    private var touchStartY = 0f
    private val cornerSize = 100 // 左上角有效区域大小（像素）
    private val longPressRunnable = Runnable {
        isLongPress = true
        showConfigDialog()
    }

    companion object {
        private const val PREF_NAME = "AppConfig"
        private const val KEY_SAVED_URI = "saved_uri"
        private const val KEY_SCREEN_ORIENTATION = "screen_orientation"
        private const val LONG_PRESS_DURATION = 5000L // 5 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI() // 启动即全屏
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        applyScreenOrientation()

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        setupWebView()
        loadConfiguredUri()
    }

    // 使用 dispatchTouchEvent 来可靠地检测左上角长按
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.rawX
                touchStartY = event.rawY
                isLongPress = false
                // 只有在左上角区域才启动长按检测
                if (event.rawX < cornerSize && event.rawY < cornerSize) {
                    handler.postDelayed(longPressRunnable, LONG_PRESS_DURATION)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // 如果手指移动超过一定距离，取消长按
                val dx = Math.abs(event.rawX - touchStartX)
                val dy = Math.abs(event.rawY - touchStartY)
                if (dx > 20 || dy > 20) {
                    handler.removeCallbacks(longPressRunnable)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        // 启用沉浸式模式
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun applyScreenOrientation() {
        // 默认为横屏 (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE = 0)
        val orientation = sharedPreferences.getInt(KEY_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        requestedOrientation = orientation
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

        // 长按检测已移至 dispatchTouchEvent 方法中实现
    }

    private fun showConfigDialog() {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        // URI 输入框
        val uriLabel = TextView(context).apply { text = "网址 (URI):" }
        val inputUri = EditText(context).apply {
            setText(getConfiguredUri())
            setSelection(text.length)
        }
        
        // 屏幕方向选择
        val orientationLabel = TextView(context).apply { 
            text = "\n屏幕方向:" 
            setPadding(0, 20, 0, 10)
        }
        
        val radioGroup = RadioGroup(context).apply {
            orientation = RadioGroup.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val rbLandscape = RadioButton(context).apply {
            text = "横屏"
            id = View.generateViewId()
        }
        
        val rbPortrait = RadioButton(context).apply {
            text = "竖屏"
            id = View.generateViewId()
        }

        radioGroup.addView(rbLandscape)
        radioGroup.addView(rbPortrait)

        // 设置当前选中状态
        val currentOrientation = sharedPreferences.getInt(KEY_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            rbPortrait.isChecked = true
        } else {
            rbLandscape.isChecked = true
        }

        layout.addView(uriLabel)
        layout.addView(inputUri)
        layout.addView(orientationLabel)
        layout.addView(radioGroup)

        AlertDialog.Builder(context)
            .setTitle("应用配置")
            .setView(layout)
            .setPositiveButton("保存并重启") { _, _ ->
                val newUri = inputUri.text.toString().trim()
                val newOrientation = if (rbPortrait.isChecked) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }

                if (newUri.isNotEmpty()) {
                    saveConfig(newUri, newOrientation)
                    applyScreenOrientation() // 立即应用屏幕方向
                    webView.loadUrl(newUri)
                    Toast.makeText(context, "配置已保存", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("清除配置") { _, _ ->
                clearConfig()
                applyScreenOrientation()
                loadConfiguredUri()
                Toast.makeText(context, "已恢复默认配置", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun saveConfig(uri: String, orientation: Int) {
        sharedPreferences.edit()
            .putString(KEY_SAVED_URI, uri)
            .putInt(KEY_SCREEN_ORIENTATION, orientation)
            .apply()
    }

    private fun clearConfig() {
        sharedPreferences.edit()
            .remove(KEY_SAVED_URI)
            .remove(KEY_SCREEN_ORIENTATION)
            .apply()
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
                        <h1>🔗 BJSZ控制端</h1>
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
