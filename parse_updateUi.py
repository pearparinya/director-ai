with open("main_activity_full.kt", "r") as f:
    lines = f.readlines()
for i, l in enumerate(lines[812:830]):
    print(f"{i+812}: {l.rstrip()}")
