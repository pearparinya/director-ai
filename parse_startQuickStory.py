with open("main_activity_full.kt", "r") as f:
    lines = f.readlines()
for i, l in enumerate(lines[647:670]):
    print(f"{i+647}: {l.rstrip()}")
