import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace('targetCompatibility = JavaVersion.VERSION_11\n  }', 'targetCompatibility = JavaVersion.VERSION_11\n  }\n  kotlinOptions {\n    jvmTarget = "11"\n  }')

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
