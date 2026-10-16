import re

with open("main_activity_full.kt", "r") as f:
    content = f.read()

# find onCreate block
start = content.find("fun onCreate(savedInstanceState: Bundle?)")

# match until the end of onCreate block
brace_count = 0
in_on_create = False
end = -1
for i, c in enumerate(content[start:]):
    if c == '{':
        brace_count += 1
        in_on_create = True
    elif c == '}':
        brace_count -= 1

    if in_on_create and brace_count == 0:
        end = start + i + 1
        break

print(f"onCreate start: {start}, end: {end}")
print("Last 10 lines of onCreate:")
print(content[end-300:end])
