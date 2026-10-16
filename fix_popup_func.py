import re

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "r") as f:
    content = f.read()

# We need to add showChannelPopup() function so it doesn't crash on compilation
dummy_func = """    private fun showChannelPopup() {
        // Will implement soon
    }"""

# Inject before updateUi()
content = content.replace("private fun updateUi()", dummy_func + "\n\n    private fun updateUi()")

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "w") as f:
    f.write(content)
