import re

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "r") as f:
    content = f.read()

# We need to replace the old sceneLabel / timeLabel etc. and introduce the status pill
# Let's find what is there currently.

old_middle = """timeLabel = TextView(this).apply {
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(2), dp(8), dp(4))
        }
        val timeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        timeRow.addView(timeLabel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val soundButton = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(gold)
            text = if (statePrefs.getBoolean("soundEnabled", true)) "🔊" else "🔇"
            setOnClickListener {
                soundEnabled = !soundEnabled
                text = if (soundEnabled) "🔊" else "🔇"
                statePrefs.edit().putBoolean("soundEnabled", soundEnabled).apply()
                if (soundEnabled) playTone(ToneGenerator.TONE_PROP_BEEP, 70)
            }
        }
        timeRow.addView(soundButton, LinearLayout.LayoutParams(dp(42), dp(32)))
        root.addView(timeRow, full())
        startClock()

        sceneLabel = TextView(this).apply {
            text = "EP 01   •   SCENE 01 / 20   •   9:16 (8 SEC)"
            textSize = 16f
            setTextColor(ice)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setBackgroundColor(panel)
        }
        root.addView(sceneLabel, full())

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 20
            progress = 1
        }
        root.addView(progress, full(dp(7)))"""

new_middle = """// Let's keep the sound button logic but maybe put it elsewhere or keep it for now.
        // Actually, we can move the sound button to a corner or keep it in the footer.
        // Let's create the status pill.
        val statusPill = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(8))
            val bgDrawable = android.graphics.drawable.GradientDrawable()
            bgDrawable.setColor(pillBg)
            bgDrawable.cornerRadius = dp(24).toFloat()
            background = bgDrawable
        }

        sceneLabel = TextView(this).apply {
            text = "🎬 พร้อมสร้างเรื่องใหม่ • EP 01 (SCENE 01/20)"
            textSize = 14f
            setTextColor(Color.parseColor("#14161F")) // dark color
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        statusPill.addView(sceneLabel, full())

        status = TextView(this).apply {
            text = "ขั้นตอน 1 • เลือกเรื่องแล้วส่งไป Gemini"
            textSize = 13f
            setTextColor(pillText)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        statusPill.addView(status, full())

        root.addView(statusPill, full().apply { setMargins(0, 0, 0, dp(24)) })

        // Progress bar hidden or modified, let's just keep it hidden since timeline replaces it visually
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 20
            progress = 1
            visibility = android.view.View.GONE
        }
        root.addView(progress, full(dp(7)))

        timeLabel = TextView(this) // Dummy
"""

if old_middle in content:
    content = content.replace(old_middle, new_middle)
    print("Replaced middle!")
else:
    print("Not found middle!")

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "w") as f:
    f.write(content)
