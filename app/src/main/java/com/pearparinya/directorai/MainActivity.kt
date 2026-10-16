package com.pearparinya.directorai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var currentEp = 1
    private var currentScene = 1
    private var selectedCategory: String? = null
    private var activeStep = 1
    private var storyStartTimeMs = 0L
    private var storyEndTimeMs = 0L
    private var soundEnabled = true
    private var shareInProgress = false
    private var lastShareAtMs = 0L
    private var awaitingGeminiReturn = false

    private lateinit var timeLabel: TextView
    private val clockHandler = Handler(Looper.getMainLooper())
    private val stepButtons = mutableMapOf<Int, Button>()

    private lateinit var sceneLabel: TextView
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var input: EditText
    private lateinit var titlePanel: LinearLayout
    private lateinit var nextHint: TextView
    private lateinit var characterMasterButton: Button
    private var pendingCharacterKey: String? = null

    private val characterNames = linkedMapOf(
        "gawin" to "กวิน",
        "rinlada" to "รินลดา",
        "mind" to "มายด์"
    )

    private val characterPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val key = pendingCharacterKey
        pendingCharacterKey = null
        if (uri != null && key != null) {
            saveCharacterMaster(key, uri)
        }
    }

    private val navy = Color.rgb(12, 15, 38)
    private val panel = Color.rgb(37, 32, 78)
    private val gold = Color.rgb(255, 190, 74)
    private val ice = Color.rgb(248, 247, 255)
    private val muted = Color.rgb(196, 193, 222)
    private val success = Color.rgb(83, 224, 210)
    private val activeGreen = Color.rgb(46, 204, 113)
    private val flowBlue = Color.rgb(41, 128, 185)

    private val statePrefs by lazy {
        getSharedPreferences("director_ai_gemini_state", Context.MODE_PRIVATE)
    }

    override fun onResume() {
        super.onResume()
        if (awaitingGeminiReturn || statePrefs.getBoolean("awaitingGeminiReturn", false)) {
            awaitingGeminiReturn = false
            statePrefs.edit().putBoolean("awaitingGeminiReturn", false).apply()
            if (::status.isInitialized) {
                status.text = "✓ กลับสู่ DIRECTOR AI • โหลดรูปจาก Gemini แล้ว กดปุ่ม ❹ เพื่อส่งเข้า Flow"
            }
            if (::nextHint.isInitialized) {
                nextHint.text = "ได้รูป 9:16 แล้ว • กด ❹ ทำวิดีโอใน Flow หรือกด ❸ ไปฉากถัดไป"
            }
            saveWorkState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkForAutoUpdateFromGitHub(this)

        val scroll = ScrollView(this).apply {
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#0B0C1E"), Color.parseColor("#121024"))
            )
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(42), dp(14), dp(10))
        }

        val headerCard = android.widget.FrameLayout(this).apply {
            setPadding(dp(2), dp(2), dp(2), dp(2))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(navy)
                setStroke(dp(2), gold)
                cornerRadius = dp(20).toFloat()
            }
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        val profileContainer = android.widget.FrameLayout(this).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setStroke(dp(2), gold)
            }
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(16).toFloat())
                }
            }
        }

        profileContainer.addView(ImageView(this).apply {
            setImageResource(R.drawable.app_custom_icon)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }, android.widget.FrameLayout.LayoutParams(dp(84), dp(84)))

        headerRow.addView(profileContainer, LinearLayout.LayoutParams(dp(84), dp(84)).apply { marginEnd = dp(14) })

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

        headerRow.addView(headerText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        headerCard.addView(headerRow)
        root.addView(headerCard, full())

        timeLabel = TextView(this).apply {
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


        // Recreate essential variables needed by existing code
        sceneLabel = TextView(this)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 20; progress = 1 }
        input = EditText(this).apply { visibility = android.view.View.GONE }
        titlePanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = android.view.View.GONE }
        characterMasterButton = Button(this).apply { setOnClickListener { showCharacterMasterSetup() } }
        nextHint = TextView(this)
        status = TextView(this) // We will use status text inside the pill

        // 1. Status Pill
        val statusPill = TextView(this).apply {
            text = "🎬 พร้อมสร้างเรื่องใหม่ • EP 01 (SCENE 01/20)"
            textSize = 13f
            setTextColor(Color.parseColor("#111526"))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#DCE4EC"))
                cornerRadius = dp(24).toFloat()
            }
        }
        root.addView(statusPill, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            setMargins(0, dp(16), 0, dp(8))
        })

        // This makes `status` and `sceneLabel` update the pill instead
        // Actually to keep it simple, we can just let existing status updates work on `status` view but keep it invisible,
        // and manually update our statusPill if needed, OR we can replace `status` with this pill. Let's replace `status`.
        status = statusPill

        val timelineTitle = TextView(this).apply {
            text = "ขั้นตอน 1 • เลือกเรื่องแล้วส่งไป Gemini"
            textSize = 16f
            setTextColor(Color.parseColor("#E5A93C"))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(16))
        }
        root.addView(timelineTitle, full())
        sceneLabel = timelineTitle // hook sceneLabel to timelineTitle just to avoid null pointer, though text format differs. Wait, updateUi does `sceneLabel.text = "EP..."`. Let's create an invisible sceneLabel to catch those updates, and update our UI appropriately in updateUi. Let's just use the invisible ones.

        // Let's reset the variables so they don't crash
        sceneLabel = TextView(this).apply { visibility = android.view.View.GONE; root.addView(this) }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { visibility = android.view.View.GONE; root.addView(this) }
        input = EditText(this).apply { visibility = android.view.View.GONE; root.addView(this) }
        titlePanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = android.view.View.GONE; root.addView(this) }
        characterMasterButton = Button(this).apply { visibility = android.view.View.GONE; root.addView(this) }
        nextHint = TextView(this).apply { visibility = android.view.View.GONE; root.addView(this) }

        // 2. Vertical Stepper Timeline
        val stepperContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun createStep(stepNumber: Int, title: String, subtitle: String, activeBg: String, inactiveBg: String, strokeColor: String, action: () -> Unit): android.view.View {
            val stepRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

            // Left column (number & line)
            val leftCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(8), 0, dp(12), 0)
            }

            val numberCircle = TextView(this).apply {
                text = stepNumber.toString()
                textSize = 14f
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(if(stepNumber == 1) Color.parseColor("#0088FF") else Color.parseColor("#3A3F58"))
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                }
            }
            leftCol.addView(numberCircle, LinearLayout.LayoutParams(dp(28), dp(28)))

            if (stepNumber < 4) {
                val line = android.view.View(this).apply {
                    setBackgroundColor(if(stepNumber == 1) Color.parseColor("#0088FF") else Color.parseColor("#3A3F58"))
                }
                leftCol.addView(line, LinearLayout.LayoutParams(dp(2), dp(60)).apply { setMargins(0, dp(4), 0, dp(4)) })
            }

            stepRow.addView(leftCol)

            // Right column (Card)
            val rightCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor(if(stepNumber == 1) activeBg else inactiveBg))
                    if (stepNumber == 1) setStroke(dp(1), Color.parseColor(strokeColor))
                    cornerRadius = dp(18).toFloat()
                }
                setOnClickListener { action() }
            }

            val headerText = if(stepNumber == 1) "ขั้นตอน 1 • พร้อมใช้งาน" else if(stepNumber == 2) "ขั้นตอน 2 • พร้อมใช้งาน" else "ขั้นตอน $stepNumber • ล็อก"

            rightCard.addView(TextView(this).apply {
                text = headerText
                textSize = 11f
                setTextColor(Color.parseColor(if(stepNumber == 1) "#00A8FF" else "#9AA4B2"))
                setTypeface(typeface, Typeface.BOLD)
            })

            rightCard.addView(TextView(this).apply {
                text = title
                textSize = 14f
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(2))
            })

            rightCard.addView(TextView(this).apply {
                text = subtitle
                textSize = 12f
                setTextColor(Color.parseColor("#A0AAB8"))
            })

            // Store step buttons so refreshStepIndicator can update them
            stepButtons[stepNumber] = Button(this) // dummy

            stepRow.addView(rightCard, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            return stepRow.apply { setPadding(0, 0, 0, dp(12)) }
        }

        stepperContainer.addView(createStep(1, "สร้างเรื่องและส่งไป Gemini ›", "ตรวจ MASTER → เลือกช่องและแนว → ส่งคำสั่งไป Gemini", "#122244", "#181C26", "#00A8FF") { showChannelSelectionDialog() })
        stepperContainer.addView(createStep(2, "ส่ง Gemini สร้างโครงเรื่อง + รูปฉาก 01 ›", "สร้างโครงเรื่องอัตโนมัติ → สร้างภาพเปิดฉากแรก 9:16", "#181C26", "#181C26", "#00000000") { startQuickStory() })
        stepperContainer.addView(createStep(3, "สร้างรูปฉากถัดไป • NEXT SCENE 🔒", "ส่ง Gemini สร้างภาพต่อเนื่องรายฉากจนครบตอน", "#181C26", "#181C26", "#00000000") { generateNextQuickScene() })
        stepperContainer.addView(createStep(4, "คัดลอกคำสั่งวิดีโอ + เปิด Flow / Veo 🔒", "ใช้ภาพและคำสั่งสร้างคลิปใน Google Flow", "#181C26", "#181C26", "#00000000") { openGoogleFlowWithPrompt() })

        root.addView(stepperContainer, full())


        // 3. Grid 2x3 Shortcuts
        val gridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#14161F"))
                setStroke(dp(1), Color.parseColor("#2E3446"))
                cornerRadius = dp(16).toFloat()
            }
        }

        fun createGridButton(textValue: String, action: () -> Unit): Button {
            return Button(this).apply {
                text = textValue
                textSize = 14f
                setTextColor(gold)
                setTypeface(typeface, Typeface.BOLD)
                background = null // Transparent background
                setPadding(dp(8), dp(12), dp(8), dp(12))
                gravity = Gravity.CENTER
                setOnClickListener { action() }
            }
        }

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(createGridButton("⚙️ ตั้งค่า / วิธีใช้งาน") { showHelp() }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row1.addView(createGridButton("✨ เริ่มเรื่องใหม่") { showChannelSelectionDialog() }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(createGridButton("🎭 คลังตัวละคร / MASTER") { showCharacterMasterSetup() }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row2.addView(createGridButton("🖼️ ทดลองสร้างรูป") { generateNextQuickScene() }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val row3 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row3.addView(createGridButton("📚 เรื่องของฉัน") { }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row3.addView(createGridButton("🏠 หน้าหลัก") { }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        gridContainer.addView(row1)
        gridContainer.addView(row2)
        gridContainer.addView(row3)

        root.addView(gridContainer, full().apply { setMargins(0, dp(16), 0, dp(24)) })

        // 4. Footer
        val footerText = TextView(this).apply {
            text = "Flow • Veo 3.1 Lite • 8 วินาที • 9:16\nพัฒนาโดย : นายปริญญา จันทร์จักษุ"
            textSize = 11f
            setTextColor(Color.parseColor("#9AA4B2"))
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(24))
        }
        root.addView(footerText, full())

        scroll.addView(root)
        restoreWorkState()
        refreshStepIndicator()
        setContentView(scroll)
    }

    private fun defaultWardrobe(key: String): String = when (key) {
        "gawin" -> "เสื้อโปโลเข้ารูปพอดีตัวสีเทาเข้มกับกางเกงขายาวสีดำ (fitted dark grey polo shirt and black trousers)"
        "rinlada" -> "เสื้อคอวีผ้ายืดเข้ารูปพอดีตัวสีชมพูอ่อน กับกางเกงขายาวสีครีม (snug-fitting soft pink v-neck top that naturally contours her original upper-body volume and hourglass waist)"
        "mind" -> "เสื้อคอกว้างผ้ายืดเข้ารูปพอดีตัวสีครีม กับกางเกงขายาวสีเบจ (form-fitting cream scoop-neck top that naturally contours her original upper-body volume and hourglass waist)"
        else -> "ชุดลำลองเข้ารูปพอดีตัวตามสัดส่วนเดิม"
    }

    private fun currentWardrobe(key: String): String =
        statePrefs.getString("wardrobe_current_$key", null)?.takeIf { it.isNotBlank() }
            ?: defaultWardrobe(key).also {
                statePrefs.edit().putString("wardrobe_current_$key", it).apply()
            }

    private fun explicitStoryWardrobeText(scene: Int): String =
        activeSceneCastKeys(scene).joinToString("\n") { key ->
            "- ${characterNames[key] ?: key}: สวม${currentWardrobe(key)}"
        }

    private fun buildGeminiStartCommand(story: String, category: String): String {
        return """
สร้างโครงเรื่องย่อและสร้างภาพฉากละครไทยสมจริง ขนาดแนวตั้ง 9:16 สำหรับเรื่อง “$story” หมวด “$category” (EP 01 • SCENE 01)

อ้างอิงตัวละครจากรูปที่แนบตามลำดับ:
- รูปที่ 1 = กวิน (ผู้ชาย)
- รูปที่ 2 = รินลดา (ผู้หญิง)
- รูปที่ 3 = มายด์ (ผู้หญิง)

กฎเหล็กในการสร้างภาพ (STRICT 9:16 & SILHOUETTE LOCK):
1. ขนาดภาพ: ภาพแนวตั้ง 9:16 เต็มจอมือถือเท่านั้น (Vertical 9:16 aspect ratio ห้ามสร้างภาพจัตุรัส) มุมกล้องระดับเอวขึ้นไป (Waist-up medium shot)
2. ล็อกใบหน้าและทรงผมเดิม: คงโครงหน้า ตา จมูก ปาก สีผิว ทรงผม และสีผมตามรูปต้นฉบับของแต่ละคนอย่างเคร่งครัด ห้ามเปลี่ยนหน้า
3. ล็อกสรีระและสัดส่วนช่วงบน (Body & Bustline Volume Lock): รักษาโครงสร้างสรีระทรงนาฬิกาทราย ปริมาตรสัดส่วนช่วงอก ช่วงไหล่ และเอวคอดตามรูปต้นฉบับครบถ้วน (strictly preserve exact voluptuous hourglass physique, prominent upper-body volume, bustline contour, and waist ratio from the attached reference images without flattening or slimming down)
4. เสื้อผ้าใหม่ (Form-Fitting Wardrobe): เปลี่ยนชุดจากในรูปต้นฉบับเป็นชุดลำลองในบ้านที่เนื้อผ้าเข้ารูปพอดีตัว ทิ้งตัวแนบรับกับสัดส่วนหน้าอกและเอวเดิมตามธรรมชาติ:
${explicitStoryWardrobeText(1)}
5. เหตุการณ์และฉาก (SCENE 01): ${storyBeat(1).action} (ลบฉากหลังสีเขียวออกทั้งหมด ใช้ฉากบ้านพักอาศัยสมจริง แสงธรรมชาติ)
        """.trimIndent()
    }

    private fun buildGeminiSceneCommand(): String {
        val cast = activeSceneCastNames(currentScene)
        val wardrobe = explicitStoryWardrobeText(currentScene)
        val action = storyBeat(currentScene).action
        val memory = storyMemoryText()

        return """
สร้างภาพถ่ายฉากละครไทยสมจริง ขนาดแนวตั้ง 9:16 (EP ${fmt(currentEp)} • SCENE ${fmt(currentScene)}) โดยอ้างอิงตัวละครจากรูปต้นฉบับที่แนบ

ตัวละครในฉากนี้: $cast

กฎเหล็กในการสร้างภาพ (STRICT 9:16 & SILHOUETTE LOCK):
1. ขนาดภาพ: แนวตั้ง 9:16 เต็มจอมือถือเท่านั้น (Vertical 9:16 aspect ratio ห้ามสร้างภาพจัตุรัส) มุมกล้องระดับเอวขึ้นไป (Waist-up medium shot)
2. ล็อกใบหน้าและทรงผม: คงโครงหน้า ตา จมูก ปาก สีผิว และทรงผมเดิมจากรูปที่แนบของแต่ละคนอย่างแม่นยำ
3. ล็อกสรีระและสัดส่วนช่วงบน: รักษาสรีระทรงนาฬิกาทราย ปริมาตรสัดส่วนช่วงอก ช่วงไหล่ และเอวตามรูปต้นฉบับครบถ้วน (strictly preserve exact voluptuous hourglass physique, prominent upper-body volume, bustline contour, and waist ratio from the reference images without flattening)
4. การแต่งกาย (เนื้อผ้าเข้ารูปพอดีตัวรับกับสัดส่วนช่วงบนตามรูปต้นฉบับ):
$wardrobe
5. ความต่อเนื่องของเรื่อง:
$memory
6. การกระทำและฉากปัจจุบัน:
$action
7. แสงและบรรยากาศ: ฉากภายในบ้านสมจริง แสงธรรมชาติ ภาพคมชัดระดับภาพยนตร์ ลบฉากหลังสีเขียวออกทั้งหมด
        """.trimIndent()
    }

    private fun buildFlowVideoPrompt(): String {
        val action = storyBeat(currentScene).action
        return """
Cinematic 9:16 vertical Thai drama video, 8 seconds, static locked-off camera.
Scene action: $action
Natural realistic human movement, subtle breathing, authentic facial expressions and lip movement during conversation, soft indoor lighting, photorealistic 4k quality. Maintain exact character appearance from the input image.
        """.trimIndent()
    }

    private fun openGoogleFlowWithPrompt() {
        val flowPrompt = buildFlowVideoPrompt()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("FLOW_VEO_PROMPT", flowPrompt))

        playTone(ToneGenerator.TONE_PROP_ACK, 90)
        status.text = "✓ คัดลอกคำสั่งวิดีโอ SCENE ${fmt(currentScene)} แล้ว • กำลังเปิด Google Flow"
        Toast.makeText(
            this,
            "✅ คัดลอกคำสั่งวิดีโอแล้ว! เลือกรูปจาก Gemini แล้วกดวาง (Paste) ใน Flow ได้เลย",
            Toast.LENGTH_LONG
        ).show()

        try {
            val flowIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://flow.google.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.android.chrome")
            }
            if (flowIntent.resolveActivity(packageManager) != null) {
                startActivity(flowIntent)
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://flow.google.com")))
            }
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://flow.google.com")))
        }
    }

    private fun shareToGemini(text: String, characterKeys: List<String>) {
        hideKeyboard()

        val nowMs = System.currentTimeMillis()
        if (shareInProgress || nowMs - lastShareAtMs < 1500L) {
            status.text = "⏳ กำลังส่งคำสั่ง • กรุณารอสักครู่"
            return
        }
        shareInProgress = true
        lastShareAtMs = nowMs

        val masterUris = try {
            characterMasterUris(characterKeys)
        } catch (e: Exception) {
            shareInProgress = false
            status.text = "⚠ แนบรูป MASTER ไม่สำเร็จ"
            return
        }

        if (masterUris.isEmpty()) {
            shareInProgress = false
            status.text = "⚠ กรุณาตั้งค่ารูป CHARACTER MASTER ให้ครบก่อน"
            return
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("GEMINI_PROMPT", text))

        val sendAction = if (masterUris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
        val geminiPackage = "com.google.android.apps.bard"

        val sendIntent = Intent(sendAction).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (masterUris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, masterUris.first())
                clipData = ClipData.newUri(contentResolver, "MASTER", masterUris.first())
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, masterUris)
                val clip = ClipData.newUri(contentResolver, "MASTERS", masterUris.first())
                masterUris.drop(1).forEach { uri -> clip.addItem(ClipData.Item(uri)) }
                clipData = clip
            }
            masterUris.forEach { uri ->
                try {
                    grantUriPermission(geminiPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }
        }

        try {
            val directGeminiIntent = Intent(sendIntent).apply {
                setPackage(geminiPackage)
            }
            saveWorkState()
            awaitingGeminiReturn = true
            statePrefs.edit().putBoolean("awaitingGeminiReturn", true).apply()

            if (directGeminiIntent.resolveActivity(packageManager) != null) {
                startActivity(directGeminiIntent)
                status.text = "↗ ส่งรูปและคำสั่งไปที่ Gemini แล้ว (คัดลอกคำสั่งสำรองไว้ในคลิปบอร์ดแล้ว)"
            } else {
                startActivity(Intent.createChooser(sendIntent, "เลือกแอป Gemini เพื่อสร้างรูป 9:16"))
                status.text = "↗ เลือกแอป Gemini (คัดลอกคำสั่งไว้ในคลิปบอร์ดแล้ว)"
            }
        } catch (e: Exception) {
            startActivity(Intent.createChooser(sendIntent, "เลือกแอป Gemini"))
        } finally {
            Handler(Looper.getMainLooper()).postDelayed({ shareInProgress = false }, 2000L)
        }
    }

    private var categoryIndex = 0

    private val storyCategories = listOf(
        "บันทึกรอยร้าวชีวิตคู่",
        "เกมรักซ่อนลวง",
        "ความลับใต้ชายคา",
        "ซ่อมรักหลังบ้าน",
        "หักมุมระทึกขวัญจิตวิทยา"
    )

    private val titleBanks = listOf(
        listOf("วันที่เราเริ่มไม่เหมือนเดิม", "บ้านหลังเดิมที่ไม่มีคำว่าเรา", "คนข้างกายที่ไกลกว่าเดิม", "รอยร้าวใต้คำว่าครอบครัว", "เมื่อความเงียบเข้ามาแทนที่รัก"),
        listOf("รักนี้มีคนซ่อนอยู่", "เกมหัวใจที่ไม่มีคนชนะ", "คำโกหกในคืนที่ไว้ใจ", "คนรักหรือคนลวง", "ข้อความลับหลังคำว่ารัก"),
        listOf("ความลับหลังประตูบ้าน", "สิ่งที่ซ่อนอยู่ใต้ชายคา", "บ้านนี้มีเรื่องที่ไม่เคยพูด", "ความจริงในห้องที่ปิดตาย", "ใต้หลังคาเดียวกันคนละความจริง"),
        listOf("กลับมารักกันอีกครั้ง", "บ้านที่เรายังซ่อมได้", "ก่อนรักจะสายเกินไป", "ขอโอกาสให้คำว่าเรา", "วันที่เราเลือกเริ่มใหม่"),
        listOf("คนที่ยืนอยู่หลังฉัน", "คืนที่บ้านไม่เหมือนเดิม", "ความจริงที่จำไม่ได้", "ใครบางคนรู้ทุกอย่าง", "เมื่อคนใกล้ตัวไม่ใช่คนเดิม")
    )

    private data class StoryBeat(
        val cast: List<String>,
        val action: String,
        val discovery: String,
        val unresolved: String,
        val nextHook: String
    )

    private fun storyBeat(scene: Int): StoryBeat {
        val cycle = (scene.coerceIn(1, 20) - 1) % 10
        return when (cycle) {
            0 -> StoryBeat(
                listOf("gawin", "rinlada", "mind"),
                "กวิน รินลดา และมายด์อยู่ในโถงบ้าน ระหว่างพูดคุย โทรศัพท์ของกวินมีข้อความเข้า ทุกคนหันมองหน้าจอพร้อมกัน",
                "มีข้อความใหม่ที่เกี่ยวข้องกับเหตุการณ์ล่าสุด",
                "ยังไม่รู้ว่าข้อมูลในข้อความหมายถึงอะไร",
                "กวินต้องเปิดข้อมูลให้รินลดาและมายด์เห็น"
            )
            1 -> StoryBeat(
                listOf("gawin", "rinlada", "mind"),
                "กวินเปิดข้อความในโทรศัพท์ให้รินลดาและมายด์ดู รินลดาสังเกตเห็นรายละเอียดที่เกี่ยวข้องกับมายด์ ขณะที่มายด์มีสีหน้ากังวล",
                "รายละเอียดในข้อความเชื่อมโยงไปถึงมายด์",
                "มายด์ยังไม่ได้อธิบายความเกี่ยวข้อง",
                "รินลดาต้องถามมายด์ตรง ๆ ถึงข้อมูลในข้อความ"
            )
            2 -> StoryBeat(
                listOf("rinlada", "mind"),
                "รินลดากับมายด์อยู่ที่โต๊ะในบ้าน รินลดาถามตรง ๆ ถึงข้อมูลในโทรศัพท์ มายด์จึงวางสิ่งของสำคัญลงตรงหน้ารินลดา",
                "มายด์มีสิ่งของที่เกี่ยวข้องกับข้อมูลในโทรศัพท์",
                "ยังไม่รู้ว่าสิ่งของนั้นมาจากไหน",
                "รินลดาต้องตรวจสิ่งของและถามถึงที่มา"
            )
            3 -> StoryBeat(
                listOf("gawin", "rinlada", "mind"),
                "รินลดากำลังตรวจสิ่งของที่มายด์นำออกมา กวินเดินเข้ามาเห็น รินลดายื่นให้กวินดู ขณะที่มายด์เฝ้ามองด้วยความกังวล",
                "กวินได้รับรู้เรื่องสิ่งของที่มายด์นำออกมาแล้ว",
                "ที่มาของสิ่งของยังไม่ถูกอธิบาย",
                "กวินต้องเปรียบเทียบสิ่งของกับข้อมูลในโทรศัพท์"
            )
            4 -> StoryBeat(
                listOf("gawin", "rinlada", "mind"),
                "กวินเปรียบเทียบสิ่งของกับข้อมูลในโทรศัพท์และพบจุดที่ตรงกัน รินลดาหันไปมองมายด์เพื่อรอคำตอบ ขณะที่มายด์เริ่มอธิบาย",
                "สิ่งของตรงกับข้อมูลบางส่วนในโทรศัพท์",
                "คำอธิบายของมายด์ยังไม่ครบ",
                "มายด์ต้องเปิดเผยว่าได้รับสิ่งของมาจากใคร"
            )
            5 -> StoryBeat(
                listOf("rinlada", "mind"),
                "มายด์บอกที่มาของสิ่งของให้รินลดาฟังและชี้รายละเอียดสำคัญ รินลดาฟังอย่างตั้งใจแต่ยังมีข้อสงสัย",
                "รินลดารู้ที่มาของสิ่งของแล้ว",
                "ยังมีรายละเอียดหนึ่งที่ขัดกับข้อมูลเดิม",
                "รินลดาต้องนำข้อมูลที่ขัดกันไปถามกวิน"
            )
            6 -> StoryBeat(
                listOf("gawin", "rinlada"),
                "รินลดานำสิ่งของและข้อมูลในโทรศัพท์มาให้กวินเปรียบเทียบ ทั้งคู่พบจุดที่ไม่ตรงกันและเริ่มสงสัยว่ามีบางอย่างถูกปิดบัง",
                "กวินและรินลดาพบข้อมูลที่ขัดกัน",
                "ยังไม่รู้ว่าใครให้ข้อมูลไม่ครบ",
                "ทั้งคู่ต้องกลับไปถามมายด์พร้อมหลักฐาน"
            )
            7 -> StoryBeat(
                listOf("gawin", "rinlada", "mind"),
                "กวินกับรินลดานำหลักฐานกลับมาถามมายด์ มายด์กำลังจะตอบ แต่โทรศัพท์ของกวินมีข้อความใหม่เข้ามาอีกครั้ง",
                "มีข้อมูลใหม่เข้ามาระหว่างการเผชิญหน้า",
                "ยังไม่มีใครได้เปิดอ่านข้อความใหม่",
                "กวินต้องเปิดข้อความให้ทุกคนดู"
            )
            8 -> StoryBeat(
                listOf("gawin", "rinlada", "mind"),
                "กวินเปิดข้อความใหม่ให้รินลดาและมายด์ดู ข้อมูลทำให้รินลดาตกใจ ขณะที่มายด์มีท่าทีเหมือนจำบางอย่างได้",
                "ข้อความใหม่เปิดเผยข้อมูลที่เปลี่ยนความเข้าใจเดิม",
                "มายด์ยังไม่ได้บอกสิ่งที่เพิ่งนึกออก",
                "มายด์ต้องอธิบายข้อมูลที่เพิ่งจำได้"
            )
            else -> StoryBeat(
                listOf("gawin", "rinlada", "mind"),
                "มายด์อธิบายสิ่งที่เพิ่งนึกออกและวางหลักฐานไว้กลางโต๊ะ กวินกับรินลดาตรวจดูพร้อมกันก่อนพบเงื่อนงำใหม่",
                "ทั้งสามมีข้อมูลร่วมกันมากขึ้น",
                "รายละเอียดใหม่ยังต้องตรวจสอบ",
                "ใช้รายละเอียดใหม่นี้เป็นเหตุของฉากถัดไป"
            )
        }
    }

    private fun activeSceneCastKeys(scene: Int): List<String> = storyBeat(scene).cast

    private fun activeSceneCastNames(scene: Int): String =
        activeSceneCastKeys(scene).mapNotNull { characterNames[it] }.joinToString(" + ")

    private fun storyMemoryText(): String {
        val beat = storyBeat(currentScene)
        val previous = if (currentScene > 1) storyBeat(currentScene - 1) else null
        return buildString {
            if (previous != null) {
                append("ต่อจากฉากก่อน: ${previous.nextHook}\n")
            }
            append("สิ่งที่เพิ่งรู้: ${beat.discovery}\n")
            append("เรื่องที่ยังค้าง: ${beat.unresolved}")
        }
    }


    private fun showChannelSelectionDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#111526"))
                cornerRadius = dp(24).toFloat()
            }
        }

        // Title
        dialogView.addView(TextView(this).apply {
            text = "🎬 เลือกช่องสร้างหนัง"
            textSize = 20f
            setTextColor(gold)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        })

        // Master Setup Button
        dialogView.addView(Button(this).apply {
            text = "🎭 ตรวจ / เปลี่ยน MASTER ก่อนสร้างเรื่อง"
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#3A3F58"))
            setOnClickListener { showCharacterMasterSetup() }
        }, full().apply { setMargins(0, 0, 0, dp(8)) })

        dialogView.addView(TextView(this).apply {
            text = "ตรวจภาพต้นฉบับก่อนส่ง • เลือกช่องด้านล่างเมื่อพร้อม"
            textSize = 12f
            setTextColor(Color.parseColor("#9AA4B2"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        })

        val dialog = android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))

        // Card 1
        val card1 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(16).toFloat()
            }
            clipToOutline = true
            setOnClickListener {
                dialog.dismiss()
                generateTitles()
            }
        }

        card1.addView(ImageView(this).apply {
            setImageResource(R.drawable.banner_channel_drama)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(120)))

        val card1Text = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FFB703"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        card1Text.addView(TextView(this).apply {
            text = "🎬 สตอรี่หลังบ้าน"
            textSize = 16f
            setTextColor(Color.BLACK)
            setTypeface(typeface, Typeface.BOLD)
        })
        card1Text.addView(TextView(this).apply {
            text = "ซีรีส์สั้นดราม่าผัวเมีย"
            textSize = 13f
            setTextColor(Color.BLACK)
            setTypeface(typeface, Typeface.BOLD)
        })
        card1.addView(card1Text)
        dialogView.addView(card1, full().apply { setMargins(0, 0, 0, dp(16)) })

        // Card 2
        val card2 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(16).toFloat()
            }
            clipToOutline = true
            setOnClickListener {
                dialog.dismiss()
                generateTitles() // In real app you might switch categories, but we keep it simple here
            }
        }

        card2.addView(ImageView(this).apply {
            setImageResource(R.drawable.banner_channel_all_genres)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(120)))

        val card2Text = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#12C2E9"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        card2Text.addView(TextView(this).apply {
            text = "🎬 ระบบกำกับละครอัจฉริยะ"
            textSize = 16f
            setTextColor(Color.BLACK)
            setTypeface(typeface, Typeface.BOLD)
        })
        card2Text.addView(TextView(this).apply {
            text = "สร้างละครครบทุกแนว"
            textSize = 13f
            setTextColor(Color.BLACK)
            setTypeface(typeface, Typeface.BOLD)
        })
        card2.addView(card2Text)
        dialogView.addView(card2, full().apply { setMargins(0, 0, 0, dp(16)) })

        // Cancel button
        dialogView.addView(Button(this).apply {
            text = "ยกเลิก"
            textSize = 14f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = null
            setOnClickListener { dialog.dismiss() }
        }, full())

        dialog.show()
    }


    private fun generateTitles() {
        val category = storyCategories[categoryIndex]
        selectedCategory = category
        val titles = titleBanks[categoryIndex].shuffled().take(5)
        titlePanel.removeAllViews()
        titlePanel.visibility = android.view.View.VISIBLE

        titlePanel.addView(TextView(this).apply {
            text = "หมวดละคร • $category\nแตะเลือกชื่อเรื่อง:"
            textSize = 13f
            setTextColor(gold)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), dp(4), dp(8), dp(8))
        })

        titles.forEachIndexed { index, title ->
            titlePanel.addView(Button(this).apply {
                text = "${index + 1}.  $title"
                isAllCaps = false
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                textSize = 14f
                setTextColor(ice)
                backgroundTintList = android.content.res.ColorStateList.valueOf(navy)
                setOnClickListener {
                    input.setText(title)
                    titlePanel.visibility = android.view.View.GONE
                    setActiveStep(2)
                    saveWorkState()
                    status.text = "✓ เลือกเรื่อง “$title” แล้ว • กดปุ่ม ❷ เพื่อส่งให้ Gemini"
                }
            }, full())
        }
    }

    private fun startQuickStory() {
        if (!characterMastersReady()) {
            status.text = "⚠ ตั้งค่ารูปตัวละครหลัก 3/3 ก่อนเริ่มเรื่อง"
            showCharacterMasterSetup()
            return
        }
        val story = input.text.toString().trim()
        if (story.isBlank()) {
            status.text = "⚠ กรุณาเลือกหรือพิมพ์ชื่อเรื่องก่อน"
            return
        }
        currentEp = 1
        currentScene = 1
        storyStartTimeMs = System.currentTimeMillis()
        storyEndTimeMs = 0L
        val category = selectedCategory ?: storyCategories[categoryIndex]
        selectedCategory = category
        categoryIndex = (categoryIndex + 1) % storyCategories.size

        setActiveStep(3)
        updateUi()
        saveWorkState()
        shareToGemini(buildGeminiStartCommand(story, category), characterNames.keys.toList())
    }

    private fun generateNextQuickScene() {
        if (!characterMastersReady()) {
            showCharacterMasterSetup()
            return
        }
        if (currentScene < 20) {
            currentScene++
        } else if (currentEp < 5) {
            currentEp++
            currentScene = 1
        } else {
            storyEndTimeMs = System.currentTimeMillis()
            setActiveStep(1)
            updateUi()
            status.text = "🏁 จบครบ 5 EP (100 ฉาก) เรียบร้อยแล้ว!"
            return
        }
        updateUi()
        saveWorkState()
        shareToGemini(buildGeminiSceneCommand(), activeSceneCastKeys(currentScene))
    }

    private fun characterMasterFile(key: String): File {
        val dir = File(filesDir, "character_masters")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$key.master")
    }

    private fun characterMastersReady(): Boolean =
        characterNames.keys.all { characterMasterFile(it).exists() }

    private fun shareableCharacterMasterFile(key: String): File? {
        val source = characterMasterFile(key)
        if (!source.exists()) return null
        val exportDir = File(cacheDir, "character_master_exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val exported = File(exportDir, "${key.uppercase()}_MASTER.png")
        source.inputStream().use { input ->
            exported.outputStream().use { output -> input.copyTo(output) }
        }
        return exported
    }

    private fun characterMasterUris(keys: List<String>): ArrayList<Uri> {
        val uris = arrayListOf<Uri>()
        keys.forEach { key ->
            shareableCharacterMasterFile(key)?.let { file ->
                uris.add(FileProvider.getUriForFile(this, "${packageName}.fileprovider", file))
            }
        }
        return uris
    }

    private fun refreshCharacterMasterButton() {
        val count = characterNames.keys.count { characterMasterFile(it).exists() }
        characterMasterButton.text =
            if (count == 3) "👥 ตัวละครหลักพร้อม 3/3 • CHARACTER MASTER"
            else "👥 ตั้งค่าตัวละครหลัก $count/3 • CHARACTER MASTER"
        characterMasterButton.backgroundTintList =
            android.content.res.ColorStateList.valueOf(if (count == 3) activeGreen else gold)
    }

    private fun showCharacterMasterSetup() {
        val labels = characterNames.map { (key, name) ->
            "${if (characterMasterFile(key).exists()) "✓" else "○"} $name"
        }.toTypedArray()

        val dialog = AlertDialog.Builder(this)
            .setTitle("ตั้งค่ารูปต้นฉบับ • CHARACTER MASTER")
            .setSingleChoiceItems(labels, -1, null)
            .setNegativeButton("ปิด", null)
            .setPositiveButton("เลือกรูป", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val which = dialog.listView.checkedItemPosition
                if (which == AdapterView.INVALID_POSITION) {
                    Toast.makeText(this, "แตะเลือกชื่อตัวละครก่อนครับ", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                pendingCharacterKey = characterNames.keys.elementAt(which)
                dialog.dismiss()
                characterPicker.launch(arrayOf("image/*"))
            }
        }
        dialog.show()
    }

    private fun saveCharacterMaster(key: String, uri: Uri) {
        try {
            val target = characterMasterFile(key)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                target.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
            }
            refreshCharacterMasterButton()
            status.text = "✓ บันทึกรูปต้นฉบับ ${characterNames[key]} เรียบร้อย"
            playTone(ToneGenerator.TONE_PROP_ACK, 90)
        } catch (_: Exception) {
            status.text = "⛔ บันทึกรูปไม่สำเร็จ"
        }
    }

    private fun confirmNewStory() {
        AlertDialog.Builder(this)
            .setTitle("เริ่มเรื่องใหม่?")
            .setMessage("ต้องการรีเซ็ตกลับไปเริ่ม EP 01 SCENE 01 ใช่หรือไม่?")
            .setNegativeButton("ยกเลิก", null)
            .setPositiveButton("เริ่มเรื่องใหม่") { _, _ ->
                input.setText("")
                selectedCategory = null
                currentEp = 1
                currentScene = 1
                activeStep = 1
                storyStartTimeMs = 0L
                storyEndTimeMs = 0L
                saveWorkState()
                updateUi()
                status.text = "● พร้อมเริ่มเรื่องใหม่"
            }
            .show()
    }

    private fun showHelp() {
        AlertDialog.Builder(this)
            .setTitle("วิธีใช้งาน DIRECTOR AI (Gemini + Flow)")
            .setMessage(
                """
1. ตั้งค่ารูปตัวละครหลัก (CHARACTER MASTER) ทั้ง 3 คน
2. กดปุ่ม ❶ เลือกชื่อเรื่อง
3. กดปุ่ม ❷ แอปจะส่งรูปต้นฉบับ + คำสั่งล็อกหุ่น 9:16 ไปที่แอป Gemini เพื่อสร้างรูปฉากที่ 1
4. เมื่อโหลดรูปจาก Gemini แล้ว กดปุ่ม ❹ แอปจะคัดลอกคำสั่งวิดีโอ Veo 3.1 ที่ปลอดฟิลเตอร์และเปิดเว็บ Google Flow ให้ทันที
5. กดปุ่ม ❸ เพื่อทำฉากต่อไปเรื่อยๆ จนครบ 20 ฉาก
                """.trimIndent()
            )
            .setPositiveButton("ตกลง", null)
            .show()
    }

    private fun updateUi() {
        sceneLabel.text = "EP ${fmt(currentEp)}   •   SCENE ${fmt(currentScene)} / 20   •   9:16 (8 SEC)"
        progress.progress = currentScene
        refreshStepIndicator()
    }

    private fun setActiveStep(step: Int) {
        activeStep = step.coerceIn(1, 3)
        refreshStepIndicator()
        saveWorkState()
    }

    private fun refreshStepIndicator() {
        // Our new UI doesn't rely heavily on this old button tinting, but we can update nextHint
        if (::nextHint.isInitialized && nextHint.visibility == android.view.View.VISIBLE) {
            nextHint.text = when (activeStep) {
                1 -> "ขั้นตอน ① กดเลือกชื่อเรื่อง"
                2 -> "ขั้นตอน ② กดส่งให้ Gemini สร้างรูปฉาก 01 (ขนาด 9:16)"
                else -> "ขั้นตอน ③ สร้างรูปฉากถัดไปใน Gemini หรือกด ❹ ส่งเข้า Google Flow"
            }
        }
    }

    private fun saveWorkState() {
        statePrefs.edit()
            .putString("story", if (::input.isInitialized) input.text.toString() else "")
            .putString("category", selectedCategory)
            .putInt("ep", currentEp)
            .putInt("scene", currentScene)
            .putInt("activeStep", activeStep)
            .putLong("storyStartTimeMs", storyStartTimeMs)
            .putLong("storyEndTimeMs", storyEndTimeMs)
            .apply()
    }

    private fun restoreWorkState() {
        soundEnabled = statePrefs.getBoolean("soundEnabled", true)
        selectedCategory = statePrefs.getString("category", null)
        currentEp = statePrefs.getInt("ep", 1).coerceIn(1, 5)
        currentScene = statePrefs.getInt("scene", 1).coerceIn(1, 20)
        activeStep = statePrefs.getInt("activeStep", 1).coerceIn(1, 3)
        storyStartTimeMs = statePrefs.getLong("storyStartTimeMs", 0L)
        storyEndTimeMs = statePrefs.getLong("storyEndTimeMs", 0L)
        val savedStory = statePrefs.getString("story", "").orEmpty()
        if (savedStory.isNotBlank()) input.setText(savedStory)
        updateUi()
    }

    private fun stepAction(step: Int, title: String, subtitle: String, fn: () -> Unit): Button {
        return Button(this).apply {
            text = title
            contentDescription = subtitle
            textSize = 14f
            isAllCaps = false
            setTextColor(ice)
            setTypeface(typeface, Typeface.BOLD)
            backgroundTintList = android.content.res.ColorStateList.valueOf(panel)
            setOnClickListener { fn() }
        }.also { stepButtons[step] = it }
    }

    private fun section(textValue: String): TextView = TextView(this).apply {
        text = textValue
        textSize = 12f
        setTextColor(gold)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(6), 0, dp(2))
    }

    private fun playTone(tone: Int, durationMs: Int) {
        if (!soundEnabled) return
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 35).apply {
                startTone(tone, durationMs)
                Handler(Looper.getMainLooper()).postDelayed({ try { release() } catch (_: Exception) {} }, durationMs + 80L)
            }
        } catch (_: Exception) {}
    }

    private val clockTick = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val dateText = SimpleDateFormat("dd MMM yyyy • HH:mm:ss", Locale("th", "TH")).format(Date(now))
            timeLabel.text = "🕐 $dateText"
            clockHandler.postDelayed(this, 1000L)
        }
    }

    private fun startClock() {
        clockHandler.removeCallbacks(clockTick)
        clockHandler.post(clockTick)
    }

    override fun onDestroy() {
        clockHandler.removeCallbacks(clockTick)
        super.onDestroy()
    }

    private fun full(height: Int = LinearLayout.LayoutParams.WRAP_CONTENT) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height).apply {
            setMargins(0, dp(2), 0, dp(2))
        }

    private fun fmt(number: Int): String = number.toString().padStart(2, '0')
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun hideKeyboard() {
        val view = currentFocus ?: return
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}


fun checkForAutoUpdateFromGitHub(activity: android.app.Activity) {
    Thread {
        try {
            val apiUrl = "https://api.github.com/repos/pearparinya/director-ai/releases?t=" + System.currentTimeMillis()
            val conn = java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "DirectorAI-Updater")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val releases = org.json.JSONArray(response)
                if (releases.length() > 0) {
                    val latest = releases.getJSONObject(0)
                    val releaseTitle = latest.optString("name", latest.optString("tag_name", "DIRECTOR AI"))
                    val assets = latest.optJSONArray("assets")
                    var downloadUrl = ""
                    var updatedAt = ""
                    var assetId = 0L
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val a = assets.getJSONObject(i)
                            if (a.optString("name", "").endsWith(".apk", ignoreCase = true)) {
                                downloadUrl = a.optString("browser_download_url", "")
                                updatedAt = a.optString("updated_at", "")
                                assetId = a.optLong("id", 0L)
                                break
                            }
                        }
                    }
                    if (downloadUrl.isNotEmpty() && updatedAt.isNotEmpty()) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        val remoteTime = sdf.parse(updatedAt)?.time ?: 0L
                        val localTime = activity.packageManager.getPackageInfo(activity.packageName, 0).lastUpdateTime
                        val prefs = activity.getSharedPreferences("DirectorAIAutoUpdate", android.content.Context.MODE_PRIVATE)
                        val ignoredId = prefs.getLong("ignored_asset_id", -1L)
                        if (remoteTime > (localTime + 30000L) && assetId != ignoredId) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                if (!activity.isFinishing) {
                                    android.app.AlertDialog.Builder(activity)
                                        .setTitle("🚀 พบอัปเดตใหม่: " + releaseTitle)
                                        .setMessage("ตรวจพบการแก้ไขโค้ดและบิลด์ APK เวอร์ชันใหม่บน GitHub กดอัปเดตเลยเพื่อดาวน์โหลดและติดตั้งทับได้ทันที")
                                        .setCancelable(false)
                                        .setPositiveButton("⬇️ อัปเดตเลย") { _, _ ->
                                            try {
                                                val uri = android.net.Uri.parse(downloadUrl)
                                                val req = android.app.DownloadManager.Request(uri)
                                                req.setTitle("กำลังอัปเดต Director AI")
                                                req.setDescription("กำลังดาวน์โหลด DIRECTOR-AI-1.0.apk...")
                                                req.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                req.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "DIRECTOR-AI-1.0.apk")
                                                val dm = activity.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                                                dm.enqueue(req)
                                                activity.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                            } catch (e: Exception) {
                                            }
                                        }
                                        .setNeutralButton("ไว้ทีหลัง") { _, _ ->
                                            prefs.edit().putLong("ignored_asset_id", assetId).apply()
                                        }
                                        .show()
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
    }.start()
}
