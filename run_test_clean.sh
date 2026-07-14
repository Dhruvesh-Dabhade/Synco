gradle :app:testDebugUnitTest --tests "com.remoteaudiosync.network.ReliableChannelTest" > test_output.log 2>&1
echo "STATUS: $?" >> test_output.log
