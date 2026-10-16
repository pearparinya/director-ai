import re

with open("main_activity_full.kt", "r") as f:
    content = f.read()

# Let's extract from progress setup to the end of onCreate
start = content.find("progress = ProgressBar")
end = content.find("private fun setupCharacterMasterUI")
print(content[start:start+2500])
