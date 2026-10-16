with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "r") as f:
    content = f.read()

# Let's find the onCreate method
idx = content.find("fun onCreate(")
if idx != -1:
    end_idx = idx + 4000
    print(content[idx:end_idx])
