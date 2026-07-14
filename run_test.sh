./gradlew testDebugUnitTest --tests "com.remoteaudiosync.network.ReliableChannelTest.test Reconnect" --info | grep "java.lang.AssertionError" -A 5 -B 5
