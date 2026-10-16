import re

with open("main_activity_full.kt", "r") as f:
    content = f.read()

start = content.find("text = \"❓ วิ")
print(content[start-100:start+1000])
