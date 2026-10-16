import re

with open("main_activity_full.kt", "r") as f:
    content = f.read()

start = content.find("text = \"❹ 🎥 คัดลอกคำสั่งวิดีโอ")
print(content[start:start+1000])
