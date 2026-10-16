with open("main_activity_full.kt", "r") as f:
    content = f.read()

import re
funcs = re.findall(r'fun\s+(\w+)\(', content)
print("Functions found in MainActivity.kt:")
print(funcs)
