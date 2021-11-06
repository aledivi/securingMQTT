# Securing MQTT
MQTT is a lightweight protocol that runs over TCP/IP to transport messages in a publish subscribe paradigm. Since it was developed for IoT devices, it has no security by design at all. The scope of my thesis work at Brain Technologies is to provide a mechanism to secure MQTT. 

## Key distribution
The main problem, regardless any kind of scenario or level of security to provide, is key distribution. The idea is to provide asymmetric key pair to IoT device trough separation of knowledge: an algorithm to generate keys is divided into two parts, one stored into the IoT device and one into a smartcard (or in a server, and then obtained by using a smartphone through a secure channel). Once the IoT device has received the second part through NFC, it is able to generate the key pair.
