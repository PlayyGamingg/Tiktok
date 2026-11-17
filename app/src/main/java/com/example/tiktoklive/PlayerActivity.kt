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
        override fun run() { applyTweaks(); handler.postDelayed(this, 1500) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        goImmersive()
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        showComments = prefs.getBoolean("comments", true)
        val user = intent.getStringExtra("user") ?: ""

        web = WebView(this)
        web.setBackgroundColor(0xFF121212.toInt())
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        CookieManager.getInstance().setAcceptCookie(true)
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView?, url: String?) { applyTweaks() }
        }
        web.webChromeClient = WebChromeClient()
        setContentView(web)
        web.loadUrl("https://www.tiktok.com/@$user/live")
        handler.postDelayed(tick, 1500)
        Toast.makeText(this, "Nhấn MENU để bật/tắt bình luận", Toast.LENGTH_LONG).show()
    }

    private fun goImmersive() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goImmersive()
    }

    /** Ẩn thanh bên trái + thanh trên, kéo giao diện về dạng video bên trái / chat bên phải. */
    private fun applyTweaks() {
        val hideCmt = !showComments
        val js = """
        (function(){
          var HIDE_CMT = $hideCmt;
          var W = window.innerWidth, H = window.innerHeight;

          // 1) CSS cơ bản
          var st = document.getElementById('__tv_css');
          if(!st){ st = document.createElement('style'); st.id='__tv_css'; document.head.appendChild(st); }
          var css = 'html,body{overflow:hidden !important;background:#121212 !important}' +
            '[class*="SideNav"],[class*="DivSideNav"],[data-e2e*="side-nav"],[data-e2e="live-side-nav"],' +
            '[class*="DivHeaderContainer"],[class*="HeaderContainer"],[id="app-header"],[data-e2e="tiktok-header"]{display:none !important}';
          if(HIDE_CMT){
            css += '[data-e2e*="chat"],[data-e2e*="comment"],[class*="ChatRoom"],[class*="DivChat"],[class*="LiveComment"]{display:none !important}';
          }
          st.textContent = css;

          // 2) Tự dò thanh bên trái (không phụ thuộc tên class)
          var nodes = document.body.querySelectorAll('div,nav,aside,section');
          var hidden = [];
          for(var i=0;i<nodes.length;i++){
            var el = nodes[i];
            if(el.dataset.__tvHidden) continue;
            var r = el.getBoundingClientRect();
            if(r.left <= 2 && r.width >= 120 && r.width <= 420 && r.height >= H*0.8 && r.height <= H+4){
              var t = el.innerText || '';
              if(/LIVE|Follow|Khám phá|Explore|Nhận Xu|Thêm/i.test(t)){
                el.style.setProperty('display','none','important');
                el.dataset.__tvHidden = '1';
                hidden.push(el);
              }
            }
          }

          // 3) Xoá khoảng trống bên trái sau khi ẩn thanh bên
          if(hidden.length){
            hidden.forEach(function(h){
              var p = h.parentElement;
              while(p && p !== document.documentElement){
                var kids = p.children;
                for(var k=0;k<kids.length;k++){
                  var cs = getComputedStyle(kids[k]);
                  if(parseInt(cs.marginLeft)>100) kids[k].style.setProperty('margin-left','0','important');
                  if(parseInt(cs.paddingLeft)>100) kids[k].style.setProperty('padding-left','0','important');
                  if(cs.position==='absolute' && parseInt(cs.left)>100) kids[k].style.setProperty('left','0','important');
                }
                var pcs = getComputedStyle(p);
                if(parseInt(pcs.paddingLeft)>100) p.style.setProperty('padding-left','0','important');
                if(parseInt(pcs.marginLeft)>100) p.style.setProperty('margin-left','0','important');
                p = p.parentElement;
              }
            });
            window.dispatchEvent(new Event('resize'));
          }
        })();
        """.trimIndent()
        web.evaluateJavascript(js, null)
    }

    override fun onKeyDown(code: Int, e: KeyEvent): Boolean {
        if (code == KeyEvent.KEYCODE_MENU || code == KeyEvent.KEYCODE_1 ||
            code == KeyEvent.KEYCODE_INFO || code == KeyEvent.KEYCODE_PROG_YELLOW) {
            showComments = !showComments
            getSharedPreferences("cfg", MODE_PRIVATE).edit().putBoolean("comments", showComments).apply()
            applyTweaks()
            Toast.makeText(this, if (showComments) "Bình luận: BẬT" else "Bình luận: TẮT", Toast.LENGTH_SHORT).show()
            return true
        }
        if (code == KeyEvent.KEYCODE_BACK && web.canGoBack()) { web.goBack(); return true }
        return super.onKeyDown(code, e)
    }

    override fun onDestroy() { handler.removeCallbacks(tick); web.destroy(); super.onDestroy() }
}
