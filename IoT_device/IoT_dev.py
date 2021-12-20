def waitCard(connection):
    try:
        connection.connect()
        return True
    except:
        print("Waiting smartcard")
        return False

def readCard(connection):
    end = False
    i = 1
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
        for block in range(i*4, i*4+3): #because last block of each sector is the key
            COMMAND = [0xFF, 0xB0, 0x00]
            COMMAND.append(block)
            COMMAND.append(16)    
            data, sw1, sw2 = connection.transmit(COMMAND)
            blockdata = toHexString(data).replace(" ","")
            if(block==4): # first block
                if(blockdata[:6]!="000003"):
                    print("NO ndef format found")
                    sys.exit()
            
                #length = int(blockdata[6:8],16)
                typelength = int(blockdata[10:12],16)
                payloadlength = int(blockdata[12:14],16)
                if(typelength<=9):
                    typefield = typefield + blockdata[14:14+typelength*2]
                    if(14+typelength*2<32):
                        payload = payload + blockdata[14+typelength*2:]
                        payloadlength = payloadlength - 32 - 14 - typelength*2
                    typelength = 0
                else:
                    typefield = typefield + blockdata[14:]
                    typelength -= 9
            else:
                if(typelength>0): # not first block but typefield to read
                    if(typelength<=16):
                        if(typelength==16):
                            typefield = typefield + blockdata
                        else:
                            typefield = typefield + blockdata[:typelength*2]
                            payload = payload + blockdata[typelength*2:]
                            payloadlength = payloadlength - typelength*2
                        typelength = 0
                    else:
                        typefield = typefield + blockdata
                        typelength -= 16
                else:
                    if(payloadlength<=16):
                        payload += blockdata[:payloadlength*2-2] #discard terminator FE
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
    k2 = bytearray.fromhex(payload)
    return k2[23:], typestring
r = readers()
if len(r) < 1:
	print("error: No readers available!")
	sys.exit()

print("Available readers: ", r)

reader = r[0]
print("Using: ", reader)

connection = reader.createConnection()
#connection.connect()
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
