# WirelessController
This android app is a touch screen controller for use with retropie that communicates over bluetooth.

![Alt text](https://github.com/grimtims/WirelessController/blob/master/screenshot.png "Optional Title")

## Raspberry Pi
You will need the latest retropie image for your raspberry pi.

Install bluetooth packages,
> sudo apt-get install bluetooth blueman bluez

Make sure in /etc/systemd/system/dbus-org.bluez.service these two lines are present,
> ExecStart=/usr/lib/bluetooth/bluetoothd -C

> ExecStartPost=/usr/bin/sdptool add SP

then reboot,
> sudo reboot

You must now pair your Android phone with the raspberry pi either manually or through the desktop.

Once pairing is complete, create a file on your raspberry pi desktop called BT_Server.py, code below.

```python
#BT_Server.py
import bluetooth
server_socket=bluetooth.BluetoothSocket( bluetooth.RFCOMM )
 
port = 1
server_socket.bind(("",port))
server_socket.listen(1)
 
client_socket,address = server_socket.accept()
print "Accepted connection from ",address
while 1: 
 data = client_socket.recv(1024)
 if(data == "q"):
  print("Quit")
  break
 trim_data = data.split("_")[0]
 bin_data = "{0:016b}".format(int(trim_data, 16))
 print "Received: %s\t bin: %s" %(trim_data, bin_data)

client_socket.close()
server_socket.close()
```

you can now start this python script,
> python BT_Server.py

## Android

You can now start the WirelessController app on your android phone.

- Press **TURN ON** to turn on bluetooth if it isn't already.
- Press **CONNECT** to search and connect to *retropie*. Once connected the python script that you started earlier on the retropie will
start outputting received inputs.
