import re

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "r") as f:
    content = f.read()

# Replace ScrollView and root LinearLayout background
old_scroll_root = """val scroll = ScrollView(this).apply { setBackgroundColor(navy) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(42), dp(14), dp(10))
            setBackgroundColor(navy)
        }"""

new_scroll_root = """val scroll = ScrollView(this).apply {
            setBackgroundColor(navy)
            isSmoothScrollingEnabled = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(42), dp(14), dp(10))
            val bgDrawable = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(navy, navyLight)
            )
            background = bgDrawable
        }"""

content = content.replace(old_scroll_root, new_scroll_root)

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "w") as f:
    f.write(content)
