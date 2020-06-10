# ForegroundServiceDemo

Usage:

If you use Android phone as the server and PC as client,
1. adb forward tcp:8000 localabstract:localSver
2. Uncomment val thread = ServerThreadConnect() at MainActivity
3. Run app server
4. Run java PCClient


Alternatively, you can use your PC as server and Android phone as client,
1. adb reverse localabstract:scrcpy tcp:8000
2. Run java PCServer.
   To display video frames, you can also run a ffplay server by the following command:
       ffplay -f h264 -codec:v h264 tcp://127.0.0.1:8000?listen
3. Uncomment val thread = ClientThreadConnect()
4. Run app client


To decrease ffplay latency,
ffplay -fflags nobuffer -fflags discardcorrupt -flags low_delay -framedrop  \
-codec:v h264 tcp://127.0.0.1:8000?listen