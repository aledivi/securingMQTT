import sys
from pwn import *
from Crypto.Random import get_random_bytes
from Crypto.PublicKey import RSA
from Crypto.Protocol.KDF import PBKDF2
from knowledge import randfunc

# to simulate secret partial knowledge of IoT device
#key_c1 = get_random_bytes(128) 
f = open(sys.argv[1], "rb")
key_c1 = f.read()
f.close()
print("IoT key (C1) = " + str(key_c1))

ADDRESS = "localhost"
PORT = 47540
dev = remote(ADDRESS, PORT) # to simulate NFC (smartcard - device)
key_c2 = dev.recv(128)
print("Received key from smartcard (C2) = " + str(key_c2))
dev.close()

master_key = bytes(k1 ^ k2 for (k1, k2) in zip(key_c1, key_c2)) # to simulate completion of final knowledge
print("Master key generated: " + str(master_key))

def my_rand(n):
    # count default is 1000, suggested to use at least 1 million, 
    #return PBKDF2(master_key, get_random_bytes(16), dkLen=n, count=1)
    return randfunc(master_key,n)

RSA_key = RSA.generate(1024, randfunc=my_rand)
print(RSA_key)
print(RSA_key.export_key(format = 'PEM', pkcs=8))