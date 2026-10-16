import re

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "r") as f:
    content = f.read()

# Make sure R symbol is imported if not. We might need import com.pearparinya.directorai.R
# It should be automatically available if it's the same package, let's verify.

import_str = "import com.pearparinya.directorai.R"
if import_str not in content:
    content = content.replace("import android.os.Bundle", "import android.os.Bundle\n" + import_str)

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "w") as f:
    f.write(content)
