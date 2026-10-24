package com.plusplay.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Player settings that apply to every video, set before any launch. */
class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Player settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        prefs = getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(list)
        })

        section("Subtitles")
        stepper("Largest size", "dp", AppSettings.SUB_SIZE,
            "The biggest size used. Each phrase gets the largest size up to this at which it fits.")
        stepper("Lines in full screen", "", AppSettings.LINES_FULL,
            "Longer phrases are made smaller so they fit in this many lines.")
        stepper("Lines in picture-in-picture", "", AppSettings.LINES_PIP, null)
        stepper("Area height in full screen", "%", AppSettings.BOX_FULL,
            "Subtitles stay inside this share of the window height, at the bottom.")
        stepper("Area height in picture-in-picture", "%", AppSettings.BOX_PIP, null)

        section("Playback")
        toggle("Resume automatically", AppSettings.KEY_AUTO_RESUME, true,
            "Off: videos start from the beginning and a chip offers to resume.")
        choice("Default picture scaling", AppSettings.KEY_ASPECT_MODE,
            listOf("FIT" to "Fit", "CROP" to "Crop", "STRETCH" to "Stretch"), "FIT")

        section("Picture-in-picture")
        choice("Default size", AppSettings.KEY_PIP_SIZE, listOf("0" to "Small", "1" to "Big", "2" to "Biggest"), "0", asInt = true)
        toggle("Keep window from filling the screen on tap (experimental)", AppSettings.KEY_TAP_SHRINK, true,
            "Android 8 enlarges the window when you tap it. This switches to a narrower shape meanwhile.")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun section(text: String) {
        list.addView(TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(resources.getColor(R.color.colorPrimary))
            setPadding(0, dp(20), 0, dp(4))
        })
    }

    private fun row(label: String, hint: String?): Pair<LinearLayout, LinearLayout> {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(8)) }
        val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        line.addView(TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(Color.BLACK)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(line)
        if (hint != null) {
            box.addView(TextView(this).apply {
                text = hint
                textSize = 12f
                setTextColor(Color.GRAY)
            })
        }
        list.addView(box)
        return box to line
    }

    private fun button(text: String, onClick: () -> Unit) = TextView(this).apply {
        this.text = text
        textSize = 20f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setBackgroundColor(resources.getColor(R.color.colorPrimary))
        minWidth = dp(44)
        minHeight = dp(40)
        setOnClickListener { onClick() }
    }

    private fun gap() = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(8), 1) }

    private fun stepper(label: String, unit: String, setting: AppSettings.IntSetting, hint: String?) {
        val (_, line) = row(label, hint)
        val value = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            minWidth = dp(64)
        }
        fun refresh() { value.text = "${prefs.getInt(setting.key, setting.default)}$unit" }
        fun change(dir: Int) {
            prefs.edit().putInt(setting.key, setting.next(prefs.getInt(setting.key, setting.default), dir)).apply()
            refresh()
        }
        line.addView(button("−") { change(-1) })
        line.addView(value)
        line.addView(button("+") { change(1) })
        refresh()
    }

    private fun toggle(label: String, key: String, default: Boolean, hint: String?) {
        val (_, line) = row(label, hint)
        line.addView(Switch(this).apply {
            isChecked = prefs.getBoolean(key, default)
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean(key, on).apply() }
        })
    }

    private fun choice(label: String, key: String, options: List<Pair<String, String>>, default: String, asInt: Boolean = false) {
        val (_, line) = row(label, null)
        val value = TextView(this).apply {
            textSize = 16f
            setTextColor(resources.getColor(R.color.colorPrimary))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        fun current(): String = if (asInt) prefs.getInt(key, default.toInt()).toString() else (prefs.getString(key, default) ?: default)
        fun refresh() { value.text = options.firstOrNull { it.first == current() }?.second ?: options[0].second }
        value.setOnClickListener {
            val i = options.indexOfFirst { it.first == current() }.coerceAtLeast(0)
            val next = options[(i + 1) % options.size].first
            if (asInt) prefs.edit().putInt(key, next.toInt()).apply() else prefs.edit().putString(key, next).apply()
            refresh()
        }
        line.addView(value)
        refresh()
    }
}
