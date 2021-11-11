import sys
import socket
from pwn import *
from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_OAEP

# server receive devID from a smartphone, returning C2 in a secure channel

HOST = '' #symbolic name, meaning all available interfaces
PORT = 12343

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
print('Server socket created')

try:
    s.bind((HOST,PORT))
except socket.error as msg:
    print('Bind failed. Error Code : ' + str(msg([0])) + ' Message ' + msg[1])
    sys.exit()
print('Socket bind complete')

s.listen(10)
while 1:
    conn, addr = s.accept()
    
    devID = conn.recv(4)
    public_key = RSA.importKey(conn.recv(2048), passphrase=None)
    
    filename = "C2_" + str(int(devID))
    f = open(filename, "rb")
    key_c2 = f.read()
    print("Key (C2) for devID {} = ".format(int(devID)) + str(key_c2))
    f.close()

    #C2 must be sent in a secure way, through PSK or asymmetric key encription
    cipher_public = PKCS1_OAEP.new(public_key)
    key_encrypted = cipher_public.encrypt(key_c2)
    conn.send(key_encrypted) 
    conn.close()

s.close()


"""
s = server(12343) # simulate smartphone - server connection
conn = s.next_connection()
devID = conn.recv()
public_key = RSA.importKey(conn.recv(), passphrase=None)

filename = "C2_" + str(int(devID))
f = open(filename, "rb")
key_c2 = f.read()
print("Key (C2) for devID {} = ".format(int(devID)) + str(key_c2))
f.close()

#C2 must be sent in a secure way, through PSK or asymmetric key encription
cipher_public = PKCS1_OAEP.new(public_key)
key_encrypted = cipher_public.encrypt(key_c2)
conn.send(key_encrypted) 
conn.close()
s.close()
"""