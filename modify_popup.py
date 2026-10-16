import re

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "r") as f:
    content = f.read()

popup_code = """    private fun showChannelPopup() {
        val dialogView = ScrollView(this).apply {
            setBackgroundColor(popupBg)
            val pad = dp(20)
            setPadding(pad, pad, pad, pad)
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Title
        contentLayout.addView(TextView(this).apply {
            text = "🎬 เลือกช่องสร้างหนัง"
            textSize = 20f
            setTextColor(gold)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) })

        // Button Check Master
        val btnCheckMaster = Button(this).apply {
            text = "🎭 ตรวจ / เปลี่ยน MASTER ก่อนสร้างเรื่อง"
            textSize = 14f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#3A3F58")) // Gray
            setOnClickListener { showCharacterMasterSetup() }
        }
        contentLayout.addView(btnCheckMaster, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })

        // Subtitle
        contentLayout.addView(TextView(this).apply {
            text = "ตรวจภาพต้นฉบับก่อนส่ง • เลือกช่องด้านล่างเมื่อพร้อม"
            textSize = 12f
            setTextColor(Color.parseColor("#A0AAB8"))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) })

        // Function to create channel card
        fun createChannelCard(imgRes: Int, text1: String, text2: String, color: Int, onClick: () -> Unit): LinearLayout {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val bg = android.graphics.drawable.GradientDrawable()
                bg.setColor(Color.parseColor("#1E1424"))
                bg.cornerRadius = dp(16).toFloat()
                background = bg
                clipToOutline = true
                setOnClickListener { onClick() }
            }

            val img = ImageView(this).apply {
                setImageResource(imgRes)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            card.addView(img, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(120)))

            val bottomBar = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(color)
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }

            bottomBar.addView(TextView(this).apply {
                text = text1
                textSize = 16f
                setTextColor(Color.BLACK)
                setTypeface(typeface, Typeface.BOLD)
            })

            bottomBar.addView(TextView(this).apply {
                text = text2
                textSize = 14f
                setTextColor(Color.BLACK)
                setTypeface(typeface, Typeface.BOLD)
            })

            card.addView(bottomBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            return card
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))

        // Card 1
        val card1 = createChannelCard(R.drawable.banner_channel_drama, "🎬 สตอรี่หลังบ้าน", "ซีรีส์สั้นดราม่าผัวเมีย", Color.parseColor("#FFB703")) {
            dialog.dismiss()
            input.setText("ซีรีส์สั้นดราม่าผัวเมีย") // Pre-fill or trigger logic
            categoryIndex = 0 // Fix category index to Drama or similar logic if needed, but generateTitles will ask for category anyway
            generateTitles()
        }
        contentLayout.addView(card1, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) })

        // Card 2
        val card2 = createChannelCard(R.drawable.banner_channel_all_genres, "🎬 ระบบกำกับละครอัจฉริยะ", "สร้างละครครบทุกแนว", Color.parseColor("#12C2E9")) {
            dialog.dismiss()
            input.setText("สร้างละครครบทุกแนว")
            generateTitles()
        }
        contentLayout.addView(card2, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) })

        // Cancel Button
        val btnCancel = Button(this).apply {
            text = "ยกเลิก"
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { dialog.dismiss() }
        }
        contentLayout.addView(btnCancel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        dialogView.addView(contentLayout)

        // Show with rounded background
        val bgDrawable = android.graphics.drawable.GradientDrawable()
        bgDrawable.setColor(popupBg)
        bgDrawable.cornerRadius = dp(24).toFloat()
        dialogView.background = bgDrawable

        dialog.show()
    }"""

content = content.replace("    private fun showChannelPopup() {\n        // Will implement soon\n    }", popup_code)

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "w") as f:
    f.write(content)
