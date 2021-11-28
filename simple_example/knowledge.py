from Crypto.Protocol.KDF import PBKDF2
import secrets 

def randfunc(key, n):
    return PBKDF2(key, secrets.token_bytes(16), dkLen=n)