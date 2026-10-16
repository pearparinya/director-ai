with open("main_activity_full.kt", "r") as f:
    lines = f.readlines()
for i, l in enumerate(lines[470:500]):
    print(f"{i+470}: {l.rstrip()}")
