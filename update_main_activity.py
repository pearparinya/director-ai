import re

with open("main_activity_full.kt", "r") as f:
    content = f.read()

# Write script to generate updated main activity layout here or we can modify the onCreate function using regex/string replacement.
# But since this is entirely programmatic UI in Kotlin, it's safer to rewrite `onCreate` partially or completely via string replace.

# We will just replace the `onCreate` body with the new UI requirements.
# Wait, let's look at the imports to make sure we don't miss anything.
