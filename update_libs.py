import re

with open("gradle/libs.versions.toml", "r") as f:
    content = f.read()

versions = content.split("[libraries]")[0]
libraries = content.split("[libraries]")[1].split("[plugins]")[0]
plugins = content.split("[plugins]")[1]

new_versions = versions + 'hilt = "2.51.1"\nhiltNavigationCompose = "1.2.0"\nkotlinxSerializationJson = "1.6.3"\n'
new_libraries = libraries + 'hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }\nhilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }\nhilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }\nkotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }\n'
new_plugins = plugins + 'hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }\nkotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }\n'

with open("gradle/libs.versions.toml", "w") as f:
    f.write(new_versions + "[libraries]" + new_libraries + "[plugins]" + new_plugins)
