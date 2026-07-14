import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace('  }\n  kotlinOptions {\n    jvmTarget = "11"\n  }', '  }')

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
