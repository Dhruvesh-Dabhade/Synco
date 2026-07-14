import re

with open("app/src/main/java/com/remoteaudiosync/MainActivity.kt", "r") as f:
    content = f.read()
content = content.replace("@AndroidEntryPoint\n", "")
content = content.replace("import dagger.hilt.android.AndroidEntryPoint\n", "")
with open("app/src/main/java/com/remoteaudiosync/MainActivity.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/remoteaudiosync/app/RemoteAudioSyncApp.kt", "r") as f:
    content = f.read()
content = content.replace("@HiltAndroidApp\n", "")
content = content.replace("import dagger.hilt.android.HiltAndroidApp\n", "")
with open("app/src/main/java/com/remoteaudiosync/app/RemoteAudioSyncApp.kt", "w") as f:
    f.write(content)

