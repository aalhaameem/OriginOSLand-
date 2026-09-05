package com.example.dynamicisland

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.btnNotificationAccess).setOnClickListener {
            // এই সেটিংস স্ক্রিনে গিয়ে ইউজারকে ম্যানুয়ালি এই অ্যাপের জন্য টগল অন করতে হবে
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnOverlayAccess).setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val notifAccessOn = isNotificationServiceEnabled()
        val overlayOn = Settings.canDrawOverlays(this)

        statusText.text = buildString {
            append("নোটিফিকেশন পারমিশন: ")
            append(if (notifAccessOn) "✅ দেওয়া আছে" else "❌ দেওয়া নেই")
            append("\n\nওভারলে (উপরে দেখানোর) পারমিশন: ")
            append(if (overlayOn) "✅ দেওয়া আছে" else "❌ দেওয়া নেই")
            append("\n\nদুটো পারমিশনই দিলে, নোটিফিকেশন আসলেই স্ক্রিনের উপরে একটা পিল-শেপ আইল্যান্ড ভেসে উঠবে।")
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }
}
