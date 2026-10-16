with open("main_activity_full.kt", "r") as f:
    lines = f.readlines()
for i, l in enumerate(lines[770:790]):
    print(f"{i+771}: {l.rstrip()}")
