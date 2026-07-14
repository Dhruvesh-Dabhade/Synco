import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace('alias(libs.plugins.google.services)\n}', 'alias(libs.plugins.google.services)\n  alias(libs.plugins.hilt)\n  alias(libs.plugins.kotlin.serialization)\n}')
content = content.replace('applicationId = "com.example"', 'applicationId = "com.aistudio.remoteaudiosync.rnmc"')
content = content.replace('minSdk = 24', 'minSdk = 26')
content = content.replace('// implementation(libs.androidx.navigation.compose)', 'implementation(libs.androidx.navigation.compose)')
content = content.replace('implementation(libs.kotlinx.coroutines.core)', 'implementation(libs.kotlinx.coroutines.core)\n  implementation(libs.hilt.android)\n  "ksp"(libs.hilt.compiler)\n  implementation(libs.hilt.navigation.compose)\n  implementation(libs.kotlinx.serialization.json)')

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
