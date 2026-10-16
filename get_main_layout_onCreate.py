with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "r") as f:
    content = f.read()

start = content.find("fun onCreate(")
end = content.find("private fun setupCharacterMasterUI", start)
print(content[start:end])
