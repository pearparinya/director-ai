import re

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "r") as f:
    content = f.read()

old_header = """val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(0, 0, 0, dp(4))
        }

        headerRow.addView(ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher_foreground)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }, LinearLayout.LayoutParams(dp(72), dp(72)).apply { marginEnd = dp(10) })

        val headerText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
        }

        headerText.addView(TextView(this).apply {
            text = "DIRECTOR AI • GEMINI EDITION"
            textSize = 18f
            setTextColor(gold)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
        }, full())

        headerText.addView(TextView(this).apply {
            text = "ระบบสร้างหนังอัตโนมัติ (Gemini + Google Flow)"
            textSize = 14f
            setTextColor(muted)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
        }, full())

        headerText.addView(TextView(this).apply {
            text = "พัฒนาโดย : นายปริญญา จันทร์จักษุ"
            textSize = 11f
            setTextColor(ice)
            maxLines = 1
        }, full())

        headerText.addView(TextView(this).apply {
            text = "VERTICAL 9:16 • SILHOUETTE LOCK • VEO 3.1 ULTRA"
            textSize = 10f
            setTextColor(success)
            maxLines = 1
        }, full())

        headerRow.addView(headerText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(headerRow, full())"""

new_header = """val headerCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            val bgDrawable = android.graphics.drawable.GradientDrawable()
            bgDrawable.setColor(headerBg)
            bgDrawable.cornerRadius = dp(20).toFloat()
            bgDrawable.setStroke(dp(1), headerStroke)
            background = bgDrawable
        }

        val iconContainer = LinearLayout(this).apply {
            val bgDrawable = android.graphics.drawable.GradientDrawable()
            bgDrawable.cornerRadius = dp(20).toFloat() // corners rounded
            bgDrawable.setStroke(dp(2), gold) // gold stroke 2dp
            background = bgDrawable
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }

        iconContainer.addView(ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher_foreground)
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(18).toFloat())
                }
            }
        }, LinearLayout.LayoutParams(dp(80), dp(80)))

        headerCard.addView(iconContainer, LinearLayout.LayoutParams(dp(84), dp(84)).apply { marginEnd = dp(16) })

        val headerText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
        }

        headerText.addView(TextView(this).apply {
            text = "DIRECTOR AI • GEMINI"
            textSize = 20f
            setTextColor(gold)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
        }, full())

        headerText.addView(TextView(this).apply {
            text = "ผู้กำกับอัจฉริยะ"
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
        }, full())

        headerText.addView(TextView(this).apply {
            text = "พร้อมใช้ใน Gemini & Flow"
            textSize = 14f
            setTextColor(Color.parseColor("#A0AAB8"))
            maxLines = 1
        }, full())

        headerCard.addView(headerText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(headerCard, full().apply { setMargins(0, 0, 0, dp(16)) })"""

if old_header in content:
    content = content.replace(old_header, new_header)
    print("Replaced!")
else:
    print("Not found!")

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "w") as f:
    f.write(content)
