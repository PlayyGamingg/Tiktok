package com.example.tiktoklive

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.Toast

class PlayerActivity : Activity() {
    private lateinit var web: WebView
    private var showComments = true
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { applyComments(); handler.postDelayed(this, 2000) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        showComments = prefs.getBoolean("comments", true)
        val user = intent.getStringExtra("user") ?: ""

        web = WebView(this)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        CookieManager.getInstance().setAcceptCookie(true)
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView?, url: String?) { applyComments() }
        }
        web.webChromeClient = WebChromeClient()
        setContentView(web)
        web.loadUrl("https://www.tiktok.com/@$user/live")
        handler.postDelayed(tick, 2000)
        Toast.makeText(this, "Nhấn MENU để bật/tắt bình luận", Toast.LENGTH_LONG).show()
    }

    private fun applyComments() {
        val hide = !showComments
        val js = """
            (function(){
              var id='__hide_cmt', s=document.getElementById(id);
              if($hide){
                if(!s){ s=document.createElement('style'); s.id=id;
                  s.textContent='[data-e2e*="chat"],[data-e2e*="comment"],[class*="ChatRoom"],[class*="DivChat"],[class*="LiveComment"]{display:none !important}';
                  document.head.appendChild(s); }
              } else if(s){ s.remove(); }
            })();
        """.trimIndent()
        web.evaluateJavascript(js, null)
    }

    override fun onKeyDown(code: Int, e: KeyEvent): Boolean {
        if (code == KeyEvent.KEYCODE_MENU || code == KeyEvent.KEYCODE_1 ||
            code == KeyEvent.KEYCODE_INFO || code == KeyEvent.KEYCODE_PROG_YELLOW) {
            showComments = !showComments
            getSharedPreferences("cfg", MODE_PRIVATE).edit().putBoolean("comments", showComments).apply()
            applyComments()
            Toast.makeText(this, if (showComments) "Bình luận: BẬT" else "Bình luận: TẮT", Toast.LENGTH_SHORT).show()
            return true
        }
        if (code == KeyEvent.KEYCODE_BACK && web.canGoBack()) { web.goBack(); return true }
        return super.onKeyDown(code, e)
    }

    override fun onDestroy() { handler.removeCallbacks(tick); web.destroy(); super.onDestroy() }
}
