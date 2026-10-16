import re

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "r") as f:
    content = f.read()

# Fix updateUi() text
old_updateUi = """    private fun updateUi() {
        sceneLabel.text = "EP ${fmt(currentEp)}   •   SCENE ${fmt(currentScene)} / 20   •   9:16 (8 SEC)"
        progress.progress = currentScene
        refreshStepIndicator()
    }"""

new_updateUi = """    private fun updateUi() {
        sceneLabel.text = "🎬 สร้างเรื่อง • EP ${fmt(currentEp)} (SCENE ${fmt(currentScene)}/20)"
        progress.progress = currentScene
        refreshStepIndicator()
    }"""
content = content.replace(old_updateUi, new_updateUi)

# Let's fix refreshStepIndicator to update our new timeline UI instead of stepButtons
old_refresh = """    private fun refreshStepIndicator() {
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
    }"""

new_refresh = """    private fun refreshStepIndicator() {
        if (::status.isInitialized) {
            status.text = when(activeStep) {
                1 -> "ขั้นตอน 1 • เลือกเรื่องแล้วส่งไป Gemini"
                2 -> "ขั้นตอน 2 • ส่งโครงเรื่องให้ Gemini"
                3 -> "ขั้นตอน 3 • สร้างฉากถัดไปเรื่อยๆ"
                else -> "ขั้นตอน 4 • ส่งเข้า Google Flow"
            }
        }
    }"""

content = content.replace(old_refresh, new_refresh)

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "w") as f:
    f.write(content)
