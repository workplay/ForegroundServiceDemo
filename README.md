# ForegroundServiceDemo

Usage:

If you use Android phone as the server and PC as client,
1. adb forward tcp:8000 localabstract:localSver
2. Uncomment val thread = ServerThreadConnect() at MainActivity
3. Run app server
4. Run java PCClient


Alternatively, you can use your PC as server and Android phone as client,
1. adb reverse localabstract:scrcpy tcp:8000
2. Run java PCServer
3. Uncomment val thread = ClientThreadConnect()
4. Run app client