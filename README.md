# RxBluetoothAuto

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RxBluetoothAuto-green.svg?style=true)](https://android-arsenal.com/details/3/3911)

This project contains sample code for Bluetooth communication

This includes 

 * Rx based click events
 * Rx based Bluetooth connection
 * Auto connect feature if the connection get lost
 * "Socket error -1" issue fix


### Newly added module

NFC read using rxAndroid is also added on this project

### Gradle plugins you need
   * compile "com.polidea.rxandroidble:rxandroidble:1.0.1"
    * compile 'com.jakewharton:butterknife:7.0.1'
   *  compile 'io.reactivex:rxandroid:1.2.1'
    * compile 'io.reactivex:rxjava:1.1.6'
    * compile 'com.jakewharton.rxbinding:rxbinding:0.3.0'
    * compile 'com.github.ivbaranov:rxbluetooth:0.1.0'


```javascript
nfcReader(Tag tag) :When detected this will handle the reading
ConnectFunc()      :Will try to connect to a paired bluetooth device
reconnect()		   :If connectivity is gone will retry and establish the connection
getConnection()    :Initialize a socket and return the status
readData()         :Will read the incomming data from the connected device
unsubscribe(....)  :Handle the unsubscribtion of Observers
```


### Stuff used to make this:

 * butterknife
 * ivbaranov:rxbluetooth
 * rxbinding
