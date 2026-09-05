package com.example.dynamicisland

import android.animation.ValueAnimator
import android.app.Notification
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView

/**
 * এই সার্ভিসটাই মূল কাজ করে:
 * 1) NotificationListenerService হওয়ায় ফোনের সব নোটিফিকেশন পড়তে পারে
 *    (ইউজারকে Settings > Notification access থেকে এটা চালু করতে হবে)
 * 2) SYSTEM_ALERT_WINDOW পারমিশন থাকলে স্ক্রিনের উপরে ভাসমান ভিউ (overlay) দেখাতে পারে
 * 3) নোটিফিকেশন এলে ছোট পিল থেকে বড় হয়ে অ্যাপের আইকন + টাইটেল + টেক্সট দেখায়,
 *    কয়েক সেকেন্ড পর আবার ছোট হয়ে যায় — অনেকটা iPhone-এর Dynamic Island এর মতো।
 */
class NotificationMonitorService : NotificationListenerService() {

    private lateinit var windowManager: WindowManager
    private var islandView: View? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var collapseRunnable: Runnable? = null

    // পিল ছোট থাকা অবস্থায় সাইজ (dp)
    private val collapsedWidthDp = 120
    private val collapsedHeightDp = 34

    // নোটিফিকেশন আসলে বড় হওয়া সাইজ (dp)
    private val expandedWidthDp = 320
    private val expandedHeightDp = 68

    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        addIslandView()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        removeIslandView()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeIslandView()
    }

    private fun addIslandView() {
        if (islandView != null) return

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.island_overlay, null)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            dpToPx(collapsedWidthDp),
            dpToPx(collapsedHeightDp),
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = dpToPx(14) // স্ট্যাটাসবারের ঠিক নিচে

        try {
            windowManager.addView(view, params)
            islandView = view
            // শুরুতে টাইটেল/টেক্সট খালি রেখে শুধু ছোট পিল হিসেবে দেখাবে
            view.findViewById<View>(R.id.textContainer).visibility = View.GONE
        } catch (e: Exception) {
            // Overlay পারমিশন না থাকলে এখানে এক্সসেপশন আসতে পারে
            e.printStackTrace()
        }
    }

    private fun removeIslandView() {
        islandView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        islandView = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        // নিজের অ্যাপ বা সাইলেন্ট/লো-প্রায়োরিটি সিস্টেম নোটিফিকেশন বাদ দেওয়া
        if (sbn.packageName == packageName) return

        val notification = sbn.notification ?: return
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: appLabelFor(sbn.packageName)

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""

        val icon = appIconFor(sbn.packageName)

        mainHandler.post {
            showExpanded(title, text, icon)
        }
    }

    private fun appLabelFor(pkg: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkg
        }
    }

    private fun appIconFor(pkg: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(pkg)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun showExpanded(title: String, text: String, icon: Drawable?) {
        val view = islandView ?: return

        // আগের কোনো কোলাপ্স-টাইমার থাকলে বাতিল করা
        collapseRunnable?.let { mainHandler.removeCallbacks(it) }

        val iconView = view.findViewById<ImageView>(R.id.iconView)
        val titleView = view.findViewById<TextView>(R.id.titleView)
        val textView = view.findViewById<TextView>(R.id.textView)
        val textContainer = view.findViewById<View>(R.id.textContainer)

        icon?.let { iconView.setImageDrawable(it) }
        titleView.text = title
        textView.text = text
        textContainer.visibility = View.VISIBLE

        animateSize(
            fromW = params.width, fromH = params.height,
            toW = dpToPx(expandedWidthDp), toH = dpToPx(expandedHeightDp)
        )

        // ৪ সেকেন্ড পর আবার ছোট হয়ে যাবে
        val runnable = Runnable { collapseIsland() }
        collapseRunnable = runnable
        mainHandler.postDelayed(runnable, 4000)
    }

    private fun collapseIsland() {
        val view = islandView ?: return
        animateSize(
            fromW = params.width, fromH = params.height,
            toW = dpToPx(collapsedWidthDp), toH = dpToPx(collapsedHeightDp)
        ) {
            view.findViewById<View>(R.id.textContainer).visibility = View.GONE
        }
    }

    private fun animateSize(fromW: Int, fromH: Int, toW: Int, toH: Int, onEnd: (() -> Unit)? = null) {
        val view = islandView ?: return
        val widthAnimator = ValueAnimator.ofInt(fromW, toW)
        val heightAnimator = ValueAnimator.ofInt(fromH, toH)

        widthAnimator.addUpdateListener {
            params.width = it.animatedValue as Int
            safeUpdateView(view)
        }
        heightAnimator.addUpdateListener {
            params.height = it.animatedValue as Int
            safeUpdateView(view)
        }

        widthAnimator.duration = 350
        heightAnimator.duration = 350
        widthAnimator.interpolator = OvershootInterpolator(1.0f)
        heightAnimator.interpolator = OvershootInterpolator(1.0f)

        widthAnimator.start()
        heightAnimator.start()

        onEnd?.let { mainHandler.postDelayed(it, 360) }
    }

    private fun safeUpdateView(view: View) {
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }
}
