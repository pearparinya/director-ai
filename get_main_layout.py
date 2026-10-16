import re

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "r") as f:
    content = f.read()

# Let's try to extract from onCreate down to setupCharacterMasterUI or similar
start = content.find("fun onCreate(")
end = content.find("private fun promptForCategory(")
print(content[start:end])
