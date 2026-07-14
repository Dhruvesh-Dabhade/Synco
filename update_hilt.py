import re

with open("gradle/libs.versions.toml", "r") as f:
    content = f.read()

content = content.replace('hilt = "2.51.1"', 'hilt = "2.52.0"')

with open("gradle/libs.versions.toml", "w") as f:
    f.write(content)
