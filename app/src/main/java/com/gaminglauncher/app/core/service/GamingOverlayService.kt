package com.gaminglauncher.app.core.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.gaminglauncher.app.GamingApp
import com.gaminglauncher.app.core.model.CrosshairStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GamingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var floatingPillView: View? = null
    private var pillParams: WindowManager.LayoutParams? = null

    private var expandedHudView: View? = null
    private var hudParams: WindowManager.LayoutParams? = null

    private var crosshairView: CrosshairView? = null
    private var crosshairParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private var isCrosshairEnabled = false
    private var currentCrosshairStyle = CrosshairStyle.DOT
    private var currentCrosshairColor = Color.parseColor("#00F2FE")

    private var metricsUpdateJob: Job? = null

    companion object {
        const val ACTION_START_OVERLAY = "com.gaminglauncher.action.START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.gaminglauncher.action.STOP_OVERLAY"
        const val ACTION_TOGGLE_CROSSHAIR = "com.gaminglauncher.action.TOGGLE_CROSSHAIR"
        const val EXTRA_CROSSHAIR_STYLE = "extra_crosshair_style"
        const val EXTRA_CROSSHAIR_COLOR = "extra_crosshair_color"

        fun start(context: Context) {
            val intent = Intent(context, GamingOverlayService::class.java).apply {
                action = ACTION_START_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GamingOverlayService::class.java).apply {
                action = ACTION_STOP_OVERLAY
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, GamingApp.CHANNEL_ID_OVERLAY)
            .setContentTitle("In-Game HUD Active")
            .setContentText("Draggable gaming performance overlay is active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        startForeground(1002, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                cleanupViews()
                stopSelf()
            }
            ACTION_TOGGLE_CROSSHAIR -> {
                val styleName = intent.getStringExtra(EXTRA_CROSSHAIR_STYLE)
                if (styleName != null) {
                    currentCrosshairStyle = try {
                        CrosshairStyle.valueOf(styleName)
                    } catch (_: Exception) {
                        CrosshairStyle.DOT
                    }
                }
                val colorHex = intent.getStringExtra(EXTRA_CROSSHAIR_COLOR) ?: "#00F2FE"
                currentCrosshairColor = try {
                    Color.parseColor(colorHex)
                } catch (_: Exception) {
                    Color.parseColor("#00F2FE")
                }
                toggleCrosshairOverlay()
            }
            else -> {
                setupFloatingPill()
                startMetricsLoop()
            }
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingPill() {
        if (floatingPillView != null) return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        pillParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 200
        }

        // Programmatic lightweight pill view
        val pillLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 12, 24, 12)
            setBackgroundColor(Color.argb(230, 10, 16, 26))
            val radius = 30f
            val shape = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.argb(230, 10, 16, 26))
                cornerRadius = radius
                setStroke(2, Color.parseColor("#00F2FE"))
            }
            background = shape
        }

        val pillText = TextView(this).apply {
            id = View.generateViewId()
            text = "⚡ PING: --ms | RAM: --%"
            setTextColor(Color.parseColor("#00F2FE"))
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        pillLayout.addView(pillText)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        pillLayout.setOnTouchListener { _, event ->
            val p = pillParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isClick = false
                    }
                    p.x = initialX + deltaX
                    p.y = initialY + deltaY
                    windowManager.updateViewLayout(pillLayout, p)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        toggleExpandedHud()
                    }
                    true
                }
                else -> false
            }
        }

        floatingPillView = pillLayout
        windowManager.addView(floatingPillView, pillParams)
    }

    private fun toggleExpandedHud() {
        if (isExpanded) {
            collapseHud()
        } else {
            expandHud()
        }
    }

    private fun expandHud() {
        if (expandedHudView != null) return
        isExpanded = true

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        hudParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 30, 36, 30)
            val shape = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.argb(245, 5, 8, 14))
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#9D4EDD"))
            }
            background = shape
        }

        val title = TextView(this).apply {
            text = "⚡ GAMING HUD DIAGNOSTICS"
            setTextColor(Color.parseColor("#00F2FE"))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }
        container.addView(title)

        val statsText = TextView(this).apply {
            id = View.generateViewId()
            text = "Gathering live telemetry..."
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0, 0, 0, 16)
        }
        container.addView(statsText)

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val boostBtn = Button(this).apply {
            text = "🚀 Boost RAM"
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#00F5A0"))
            textSize = 11f
            setOnClickListener {
                GamingApp.instance.memoryMonitor.boostMemory()
                text = "✓ RAM Boosted"
            }
        }
        buttonRow.addView(boostBtn)

        val crosshairBtn = Button(this).apply {
            text = if (isCrosshairEnabled) "Crosshair: ON" else "Crosshair: OFF"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#9D4EDD"))
            textSize = 11f
            setOnClickListener {
                toggleCrosshairOverlay()
                text = if (isCrosshairEnabled) "Crosshair: ON" else "Crosshair: OFF"
            }
        }
        buttonRow.addView(crosshairBtn)

        val closeBtn = Button(this).apply {
            text = "✖ Close"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#FF0055"))
            textSize = 11f
            setOnClickListener {
                collapseHud()
            }
        }
        buttonRow.addView(closeBtn)

        container.addView(buttonRow)

        expandedHudView = container
        windowManager.addView(expandedHudView, hudParams)
    }

    private fun collapseHud() {
        if (expandedHudView != null) {
            windowManager.removeView(expandedHudView)
            expandedHudView = null
        }
        isExpanded = false
    }

    private fun toggleCrosshairOverlay() {
        if (isCrosshairEnabled) {
            removeCrosshair()
        } else {
            showCrosshair()
        }
    }

    private fun showCrosshair() {
        if (crosshairView != null) return
        isCrosshairEnabled = true

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        crosshairParams = WindowManager.LayoutParams(
            200,
            200,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        crosshairView = CrosshairView(this).apply {
            setStyle(currentCrosshairStyle, currentCrosshairColor)
        }
        windowManager.addView(crosshairView, crosshairParams)
    }

    private fun removeCrosshair() {
        if (crosshairView != null) {
            windowManager.removeView(crosshairView)
            crosshairView = null
        }
        isCrosshairEnabled = false
    }

    private fun startMetricsLoop() {
        metricsUpdateJob?.cancel()
        metricsUpdateJob = serviceScope.launch {
            val app = GamingApp.instance
            while (isActive) {
                val mem = app.memoryMonitor.getMemorySnapshot()
                val cpu = app.cpuMonitor.sampleCpuUsage()
                val battery = app.batteryMonitor.getBatteryStatus()

                val pingResult = withContext(Dispatchers.IO) {
                    app.pingService.measurePing(800)
                }

                // Update floating pill
                floatingPillView?.let { pill ->
                    val textView = (pill as? LinearLayout)?.getChildAt(0) as? TextView
                    val pingStr = if (pingResult.isSuccessful) "${pingResult.latencyMs}ms" else "ERR"
                    textView?.text = "⚡ $pingStr | RAM ${mem.usagePercentage}%"
                }

                // Update expanded HUD if visible
                expandedHudView?.let { hud ->
                    val layout = hud as? LinearLayout
                    val statsText = layout?.getChildAt(1) as? TextView
                    val pingStr = if (pingResult.isSuccessful) "${pingResult.latencyMs}ms" else "Offline"
                    statsText?.text = buildString {
                        appendLine("Network Ping: $pingStr (Jitter: ${pingResult.jitterMs}ms)")
                        appendLine("Packet Loss:  ${pingResult.packetLossPercent}%")
                        appendLine("RAM Used:     ${mem.usedMb}MB / ${mem.totalMb}MB (${mem.usagePercentage}%)")
                        appendLine("CPU Usage:    $cpu%")
                        appendLine("Battery Temp: ${battery.temperatureCelsius}°C (${battery.level}%)")
                    }
                }

                delay(1200)
            }
        }
    }

    private fun cleanupViews() {
        metricsUpdateJob?.cancel()
        collapseHud()
        removeCrosshair()
        floatingPillView?.let {
            windowManager.removeView(it)
            floatingPillView = null
        }
    }

    override fun onDestroy() {
        cleanupViews()
        serviceScope.cancel()
        super.onDestroy()
    }

    class CrosshairView(context: Context) : View(context) {
        private var style: CrosshairStyle = CrosshairStyle.DOT
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        fun setStyle(newStyle: CrosshairStyle, color: Int) {
            this.style = newStyle
            this.paint.color = color
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f

            when (style) {
                CrosshairStyle.DOT -> {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, 4f, paint)
                }
                CrosshairStyle.CROSS -> {
                    paint.style = Paint.Style.STROKE
                    canvas.drawLine(cx - 20f, cy, cx - 6f, cy, paint)
                    canvas.drawLine(cx + 6f, cy, cx + 20f, cy, paint)
                    canvas.drawLine(cx, cy - 20f, cx, cy - 6f, paint)
                    canvas.drawLine(cx, cy + 6f, cx, cy + 20f, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, 2.5f, paint)
                }
                CrosshairStyle.CIRCLE -> {
                    paint.style = Paint.Style.STROKE
                    canvas.drawCircle(cx, cy, 14f, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, 3f, paint)
                }
                CrosshairStyle.CHEVRON -> {
                    paint.style = Paint.Style.STROKE
                    canvas.drawLine(cx - 14f, cy + 10f, cx, cy, paint)
                    canvas.drawLine(cx + 14f, cy + 10f, cx, cy, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy - 2f, 2.5f, paint)
                }
            }
        }
    }
}
