import sys
import socket
from pwn import *
from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_OAEP

key = RSA.generate(2048)
public_key = key.publickey()
devID = input("Please enter devID: ")

ADDRESS = "localhost"
PORT1 = 12343

client = remote(ADDRESS, PORT1)
client.send(devID)
client.send(public_key.exportKey(format='PEM', passphrase=None))
key_encrypted = client.recv()
client.close()

cipher_private = PKCS1_OAEP.new(key)
key_c2 = cipher_private.decrypt(key_encrypted)
print("Key from server = " + str(key_encrypted))
print("Key decrypted (C2) = " + str(key_c2))

HOST = ''
PORT2 = 47540

sp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
print('Socket created')

try:
    sp.bind((HOST, PORT2))
except socket.error as msg:
    print('Bind failed. Error Code : ' + str(msg[0]) + ' Message ' + msg[1])
    sys.exit()
print('Socket bind complete')

sp.listen()
print('Socket now listening')

conn, addr = sp.accept()
conn.send(key_c2)

conn.close()
sp.close()