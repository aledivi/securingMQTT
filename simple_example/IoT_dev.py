import sys
from Crypto.Random import get_random_bytes
from Crypto.PublicKey import RSA
from Crypto.Protocol.KDF import PBKDF2
from knowledge import randfunc

# to simulate secret partial knowledge of IoT device
#key_c1 = get_random_bytes(128) 
f = open(sys.argv[1], "rb")
key_c1 = f.read()
print(type(key_c1))
f.close()
print("IoT key (C1) = " + str(key_c1))

"""
ADDRESS = "localhost"
PORT = 47540
dev = remote(ADDRESS, PORT) # to simulate NFC (smartcard - device)
key_c2 = dev.recv(128)
print("Received key from smartcard (C2) = " + str(key_c2))
dev.close()
"""
key_c2 = get_random_bytes(128)
master_key = bytes(k1 ^ k2 for (k1, k2) in zip(key_c1, key_c2)) # to simulate completion of final knowledge
print("Master key generated: " + str(master_key))