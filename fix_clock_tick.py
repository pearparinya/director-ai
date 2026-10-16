import re

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "r") as f:
    content = f.read()

# We need to make sure clock stuff doesn't crash since we removed startClock() from onCreate
# Actually startClock() should still be called if we still have the clockTick task.
# Let's see if we removed startClock(). We did.
# So we need to put it back or remove clockTick altogether if it's unused.
# But wait, we replaced the part that had startClock(). Let's add it back.

fix = """        // Progress bar hidden or modified, let's just keep it hidden since timeline replaces it visually
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 20
            progress = 1
            visibility = android.view.View.GONE
        }
        root.addView(progress, full(dp(7)))

        timeLabel = TextView(this) // Dummy
        startClock()
"""

content = content.replace("""        // Progress bar hidden or modified, let's just keep it hidden since timeline replaces it visually
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 20
            progress = 1
            visibility = android.view.View.GONE
        }
        root.addView(progress, full(dp(7)))

        timeLabel = TextView(this) // Dummy
""", fix)

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "w") as f:
    f.write(content)
