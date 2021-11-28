import sys
import socket
from pwn import * 
from Crypto.Random import get_random_bytes

# to simulate partial knowledge of smartcard, the same for all devices
#key_c2 = get_random_bytes(128)
f = open(sys.argv[1], "rb")
key_c2 = f.read()
print("Smartcard key (C2) = " + str(key_c2))

HOST = '' #symbolic name, meaning all available interfaces
PORT = 47540

sc = socket.socket(socket.AF_INET, socket.SOCK_STREAM) # to simulate NFC(smartcard-device)
print('Socket created')
try:
    sc.bind((HOST,PORT))
except socket.error as msg:
    print('Bind failed. Error Code: ' + str(msg[0]) + ' Message ' + msg[1])
    sys.exit()
print('Socket bind complete')

sc.listen() 
conn, addr = sc.accept()
conn.send(key_c2)
conn.close()
sc.close()

"""
sc = server(47540) # to simulate NFC (smartcard - device)
conn = sc.next_connection()
conn.send(key_c2)

conn.close()
sc.close()
"""