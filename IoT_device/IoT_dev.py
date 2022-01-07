import secrets
from smartcard.System import readers
from smartcard.util import toHexString, toASCIIString, toBytes
import time
from Crypto.PublicKey import RSA
from Crypto.Protocol.KDF import PBKDF2
import sys
import secrets

def generateKeyPair(k1, k2):
    master_key = bytes(b1 ^ b2 for (b1, b2) in zip(k1, k2)) # to have completion of final knowledge

    def my_rand(n):
        return PBKDF2(master_key, secrets.token_bytes(16), dkLen=n)

    RSA_key = RSA.generate(1024, randfunc=my_rand)
    return RSA_key

def acquireK1():
    f = open(sys.argv[1], "rb")
    k1 = f.read()
    f.close()
    return k1

def waitCard(connection):
    try:
        connection.connect()
        return True
    except:
        print("Waiting smartcard")
        return False

def readCard(connection):
    end = False
    i = 1 # start from sector 1
    typefield = ""  
    payload = ""
    while not end:
    # decrypt first block of sector with key. if succeed, sector is unlocked
    # if other sector is unlocked, previous sector is locked
        COMMAND = [0xFF, 0x86, 0x00, 0x00, 0x05, 0x01, 0x00, i*4, 0x60, 0x00]
        data, sw1, sw2 = connection.transmit(COMMAND)
        if (sw1, sw2) == (0x90, 0x0):
            print("Status: Decryption sector "+ str(i) +" using key #0 as Key A successful.")
        elif (sw1, sw2) == (0x63, 0x0):
            print("Status: Decryption sector "+ str(i) +" failed. Trying as Key B")
            COMMAND = [0xFF, 0x86, 0x00, 0x00, 0x05, 0x01, 0x00, i*4, 0x61, 0x00]
            data, sw1, sw2 = connection.transmit(COMMAND)
        if (sw1, sw2) == (0x90, 0x0):
            print("Status: Decryption sector "+ str(i) +" using key #0 as Key B successful.")
        elif (sw1, sw2) == (0x63, 0x0):
            print("Status: Decryption sector "+ str(i) +" failed.")
            sys.exit()
	
        print("---------------------------------Sector "+ str(i) +"---------------------------------")
        for block in range(i*4, i*4+3): # because last block of each sector is the key
            COMMAND = [0xFF, 0xB0, 0x00]
            COMMAND.append(block)
            COMMAND.append(16)    
            data, sw1, sw2 = connection.transmit(COMMAND)
            blockdata = toHexString(data).replace(" ","")
            if(block==4): # first block

                ndefFormat = False
                j = 1
                while(not ndefFormat):
                    if(blockdata[2*(j-1):2*j]!="03"):
                        if(blockdata[2*(j-1):2*j]=="00"):
                            print("Ignore byte #" + str(j))
                            j = j + 1
                        else:
                            print("NO ndef format found")
                            sys.exit()
                    else:
                        ndefFormat = True
                
                length = blockdata[j*2:(j+1)*2]
                if(length=="00"):
                    print("NO value")
                    sys.exit()
                if(length=="FF"):
                    j = j + 1
                    length = int(blockdata[j*2:(j+2)*2],16)
                else:
                    length = int(blockdata[j*2:(j+1)*2],16)
                print("Total length = " + str(length))
                
                j=j+2
                typelength = blockdata[j*2:(j+1)*2]
                if(typelength=="00"):
                    print("NO type field")
                if(typelength=="FF"):
                    j = j + 1
                    typelength = int(blockdata[j*2:(j+2)*2],16)
                else:
                    typelength = int(blockdata[j*2:(j+1)*2],16)
                print("Type length = " + str(typelength))

                j = j + 1
                payloadlength = blockdata[j*2:(j+1)*2]
                if(payloadlength=="FF"):
                    j = j + 1
                    payloadlength = int(blockdata[j*2:(j+2)*2],16)
                else:
                    payloadlength = int(blockdata[j*2:(j+1)*2],16)
                print("Payload length = " + str(payloadlength))
                
                j = j + 1
                if(j<16):
                    if(typelength+j>=16):
                        typefield = typefield + blockdata[j*2:]
                    else:
                        typefield = typefield + blockdata[j*2:j*2+typelength*2]
                        payload = payload + blockdata[j*2+typelength*2:]
                        payloadlength = payloadlength - (16 - j - typelength)
                    typelength = typelength - (16 - j)
                    print("Type length = " + str(typelength))
            else:
                print("Payload length = " + str(payloadlength))
                if(typelength>0): # not first block but typefield to read
                    if(typelength<=16):
                        if(typelength==16):
                            typefield = typefield + blockdata
                        else:
                            typefield = typefield + blockdata[:typelength*2]
                            payload = payload + blockdata[typelength*2:]
                            payloadlength = payloadlength - (16-typelength)
                        typelength = 0
                    else:
                        typefield = typefield + blockdata
                        typelength -= 16
                else:
                    if(payloadlength<=16):
                        payload += blockdata[:payloadlength*2] 
                        payloadlength = 0
                        end = True
                    else:
                        payload += blockdata
                        payloadlength -= 16

            print("block "+ str(block) +":\t"+ toHexString(data) +" | "+''.join(chr(j) for j in data))

        print("Status words: %02X %02X" % (sw1, sw2))
        if (sw1, sw2) == (0x90, 0x0):
            print("Status: The operation completed successfully.")
        elif (sw1, sw2) == (0x63, 0x0):
            print("Status: The operation failed. Maybe auth is needed.")
    
        i = i+1 

    typestring = toASCIIString(toBytes(typefield))
    print(payload)
    k2 = bytearray.fromhex(payload)
    return k2[23:], typestring

k1 = acquireK1()

r = readers()
if len(r) < 1:
	print("error: No readers available!")
	sys.exit()

print("Available readers: ", r)

reader = r[0]
print("Using: ", reader)

connection = reader.createConnection()
while(True):
    if(waitCard(connection)):
        print("Smartcard revealed")
        break
    else:
        time.sleep(5)
        continue

k2, typestring = readCard(connection)
print("Type: " + typestring)
print(k2)

RSA_key = generateKeyPair(k1, k2)
print(RSA_key)
print(RSA_key.export_key(format = 'PEM', pkcs=8))