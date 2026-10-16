with open("main_activity_full.kt", "r") as f:
    lines = f.readlines()
for i, l in enumerate(lines[500:550]):
    print(f"{i+500}: {l.rstrip()}")
