import re

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "r") as f:
    content = f.read()

# Replace input, old buttons etc. with Timeline
# Wait, input field is useful. We should keep input hidden or put it somewhere, or just hide it. The instructions say to keep 100% of the functions.
# "เมื่อกดให้เปิดหน้าต่างป๊อปอัป เลือกช่องสร้างหนัง"
# If we have input, it can be kept or put inside popup. Let's hide it in main UI and show in popup or just keep it around for the functions.
# The user might want to enter story manually. Let's keep `input` hidden but initialize it so we don't break existing logic.

old_bottom = """        input = EditText(this).apply {
            hint = "ชื่อเรื่อง / คำสั่งผู้กำกับ…"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            minLines = 1
            maxLines = 2
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setBackgroundColor(panel)
        }
        root.addView(input, full().apply { setMargins(0, dp(5), 0, dp(5)) })

        root.addView(
            stepAction(1, "❶ ✨ สร้างเรื่องใหม่ • NEW STORY", "เลือกชื่อเรื่องและหมวดละคร") { generateTitles() },
            full()
        )

        titlePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = android.view.View.GONE
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(panel)
        }
        root.addView(titlePanel, full())

        characterMasterButton = Button(this).apply {
            textSize = 12f
            setTextColor(Color.BLACK)
            setTypeface(typeface, Typeface.BOLD)
            backgroundTintList = android.content.res.ColorStateList.valueOf(gold)
            setOnClickListener { showCharacterMasterSetup() }
        }
        root.addView(characterMasterButton, full(dp(40)))
        refreshCharacterMasterButton()

        root.addView(section("⚡ GEMINI IMAGE 9:16 + GOOGLE FLOW VIDEO").apply { gravity = Gravity.CENTER })

        nextHint = TextView(this).apply {
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        }
        root.addView(nextHint, full())

        root.addView(
            stepAction(2, "❷ 🎬 ส่ง Gemini สร้างโครงเรื่อง + รูปฉาก 01", "แนบ Master + ล็อกหุ่น + สร้างภาพ 9:16 ใน Gemini") { startQuickStory() },
            full()
        )

        root.addView(
            stepAction(3, "❸ ➜ ส่ง Gemini สร้างรูปฉากถัดไป • NEXT SCENE", "เปลี่ยนชุด/ฉากตามเนื้อเรื่อง คงหน้าและหุ่นเดิม 9:16") { generateNextQuickScene() },
            full()
        )

        root.addView(
            Button(this).apply {
                text = "❹ 🎥 คัดลอกคำสั่งวิดีโอ + เปิด GOOGLE FLOW"
                textSize = 14f
                isAllCaps = false
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                backgroundTintList = android.content.res.ColorStateList.valueOf(flowBlue)
                setOnClickListener { openGoogleFlowWithPrompt() }
            },
            full(dp(44)).apply { setMargins(0, dp(4), 0, dp(4)) }
        )

        status = TextView(this).apply {
            text = "● พร้อมใช้งาน • ขับเคลื่อนด้วย Gemini & Google Flow"
            textSize = 11f
            setTextColor(success)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
        }
        root.addView(status, full())

        val utilityRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        utilityRow.addView(
            Button(this).apply {
                text = "❓ วิธีใช้งาน"
                textSize = 13f
                setTextColor(Color.BLACK)
                setTypeface(typeface, Typeface.BOLD)
                backgroundTintList = android.content.res.ColorStateList.valueOf(gold)
                setOnClickListener { showHelp() }
            },
            LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(4) }
        )

        utilityRow.addView(
            Button(this).apply {
                text = "🔄 เริ่มเรื่องใหม่"
                textSize = 13f
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(210, 55, 55))
                setOnClickListener { confirmNewStory() }
            },
            LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(4) }
        )

        root.addView(utilityRow, full())"""

new_bottom = """        input = EditText(this).apply {
            hint = "ชื่อเรื่อง / คำสั่งผู้กำกับ…"
            visibility = android.view.View.GONE
        }
        root.addView(input)

        titlePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = android.view.View.GONE
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(panel)
        }
        root.addView(titlePanel, full())

        characterMasterButton = Button(this).apply { visibility = android.view.View.GONE }
        nextHint = TextView(this)

        val timelineLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun createStep(num: Int, status: String, title: String, desc: String, bgC: Int, borderC: Int?, onClick: () -> Unit): LinearLayout {
            val stepRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, dp(16))
            }

            val leftCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }

            val circle = TextView(this).apply {
                text = num.toString()
                setTextColor(Color.WHITE)
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                val bg = android.graphics.drawable.GradientDrawable()
                bg.shape = android.graphics.drawable.GradientDrawable.OVAL
                bg.setColor(if (num == 1) stepActive else stepInactive)
                background = bg
            }
            leftCol.addView(circle, LinearLayout.LayoutParams(dp(28), dp(28)))

            if (num < 4) {
                val line = android.view.View(this).apply {
                    setBackgroundColor(if (num == 1) stepActive else stepInactive)
                }
                leftCol.addView(line, LinearLayout.LayoutParams(dp(2), LinearLayout.LayoutParams.MATCH_PARENT).apply { setMargins(0, dp(4), 0, dp(4)) })
            }

            stepRow.addView(leftCol, LinearLayout.LayoutParams(dp(40), LinearLayout.LayoutParams.MATCH_PARENT))

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                val bg = android.graphics.drawable.GradientDrawable()
                bg.setColor(bgC)
                bg.cornerRadius = dp(18).toFloat()
                if (borderC != null) bg.setStroke(dp(1), borderC)
                background = bg
                setOnClickListener { onClick() }
            }

            card.addView(TextView(this).apply {
                text = status
                textSize = 11f
                setTextColor(if (num == 1 || num == 2) success else muted)
                setTypeface(typeface, Typeface.BOLD)
            })

            card.addView(TextView(this).apply {
                text = title
                textSize = 15f
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(4))
            })

            card.addView(TextView(this).apply {
                text = desc
                textSize = 12f
                setTextColor(muted)
            })

            stepRow.addView(card, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            return stepRow
        }

        timelineLayout.addView(createStep(1, "ขั้นตอน 1 • พร้อมใช้งาน", "สร้างเรื่องและส่งไป Gemini ›", "ตรวจ MASTER → เลือกช่องและแนว → ส่งคำสั่งไป Gemini", card1Bg, card1Border) {
            showChannelPopup()
        })

        timelineLayout.addView(createStep(2, "ขั้นตอน 2 • พร้อมใช้งาน", "ส่ง Gemini สร้างโครงเรื่อง + รูปฉาก 01 ›", "สร้างโครงเรื่องอัตโนมัติ → สร้างภาพเปิดฉากแรก 9:16", cardOtherBg, null) {
            startQuickStory()
        })

        timelineLayout.addView(createStep(3, "ขั้นตอน 3 • ล็อก • รอขั้นตอนก่อนหน้า", "สร้างรูปฉากถัดไป • NEXT SCENE 🔒", "ส่ง Gemini สร้างภาพต่อเนื่องรายฉากจนครบตอน", cardOtherBg, null) {
            generateNextQuickScene()
        })

        timelineLayout.addView(createStep(4, "ขั้นตอน 4 • ล็อก • รอขั้นตอนก่อนหน้า", "คัดลอกคำสั่งวิดีโอ + เปิด Flow / Veo 🔒", "ใช้ภาพและคำสั่งสร้างคลิปใน Google Flow", cardOtherBg, null) {
            openGoogleFlowWithPrompt()
        })

        root.addView(timelineLayout, full())

        // Grid 2x3
        val grid = GridLayout(this).apply {
            columnCount = 2
            rowCount = 3
            useDefaultMargins = true
            setPadding(dp(8), dp(8), dp(8), dp(8))
            val bg = android.graphics.drawable.GradientDrawable()
            bg.setColor(gridBg)
            bg.cornerRadius = dp(16).toFloat()
            bg.setStroke(dp(1), gridBorder)
            background = bg
        }

        fun gridBtn(textStr: String, action: () -> Unit): Button {
            return Button(this).apply {
                text = textStr
                textSize = 14f
                setTextColor(gridTextGold)
                setTypeface(typeface, Typeface.BOLD)
                setBackgroundColor(Color.TRANSPARENT)
                isAllCaps = false
                gravity = Gravity.CENTER
                setOnClickListener { action() }
            }
        }

        val param = { GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)) }

        grid.addView(gridBtn("⚙️ ตั้งค่า / วิธีใช้งาน") { showHelp() }, param())
        grid.addView(gridBtn("✨ เริ่มเรื่องใหม่") { showChannelPopup() }, param())
        grid.addView(gridBtn("🎭 คลังตัวละคร / MASTER") { showCharacterMasterSetup() }, param())
        grid.addView(gridBtn("🖼️ ทดลองสร้างรูป") { generateNextQuickScene() }, param())
        grid.addView(gridBtn("📚 เรื่องของฉัน") { Toast.makeText(this@MainActivity, "เรื่องปัจจุบัน: ${selectedCategory ?: "ยังไม่เลือก"}", Toast.LENGTH_SHORT).show() }, param())
        grid.addView(gridBtn("🏠 หน้าหลัก") { scroll.smoothScrollTo(0, 0) }, param())

        root.addView(grid, full().apply { setMargins(0, dp(16), 0, dp(24)) })

        // Footer Text
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        footer.addView(TextView(this).apply {
            text = "Flow • Veo 3.1 Lite • 8 วินาที • 9:16"
            textSize = 12f
            setTextColor(footerText)
            gravity = Gravity.CENTER
        })
        footer.addView(TextView(this).apply {
            text = "พัฒนาโดย : นายปริญญา จันทร์จักษุ"
            textSize = 12f
            setTextColor(footerText)
            gravity = Gravity.CENTER
        })
        root.addView(footer, full())"""

if old_bottom in content:
    content = content.replace(old_bottom, new_bottom)
    print("Replaced timeline and bottom grid!")
else:
    print("Not found bottom!")

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "w") as f:
    f.write(content)
