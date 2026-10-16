with open("app/build.gradle.kts", "r") as f:
    content = f.read()

if "buildConfig = true" not in content:
    content = content.replace("android {", "android {\n    buildFeatures {\n        buildConfig = true\n    }\n")
    with open("app/build.gradle.kts", "w") as f:
        f.write(content)
