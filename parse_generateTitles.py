with open("main_activity_full.kt", "r") as f:
    lines = f.readlines()
for i, l in enumerate(lines[608:630]):
    print(f"{i+608}: {l.rstrip()}")
