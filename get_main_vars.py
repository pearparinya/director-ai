with open("main_activity_full.kt", "r") as f:
    lines = f.readlines()
for i, l in enumerate(lines[:100]):
    print(f"{i}: {l.rstrip()}")
