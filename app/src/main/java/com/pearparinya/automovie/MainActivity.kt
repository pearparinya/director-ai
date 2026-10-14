package com.pearparinya.automovie

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

        val scroll = ScrollView(this).apply {
            setBackgroundColor(navy)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(42), dp(14), dp(10))
            setBackgroundColor(navy)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(0, 0, 0, dp(4))
        }

        headerRow.addView(ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher_foreground)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }, LinearLayout.LayoutParams(dp(72), dp(72)).apply {
            marginEnd = dp(10)
        })

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
        root.addView(headerRow, full())

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
        root.addView(progress, full(dp(7)))

        input = EditText(this).apply {
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

        root.addView(utilityRow, full())

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
3. กดปุ่ม ❷ แอปจะส่งรูปต้นฉบับ + คำสั่งล็อกหุ่น 9:16 ไปที่แอป Gemini เพื่อสร้างรูปฉากที่ 1 (และคัดลอกคำสั่งไว้ในคลิปบอร์ดให้ด้วย)
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
        stepButtons.forEach { (step, button) ->
            val active = step == activeStep
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(if (active) activeGreen else panel)
            button.setTextColor(if (active) Color.BLACK else ice)
        }
        if (::nextHint.isInitialized) {
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
