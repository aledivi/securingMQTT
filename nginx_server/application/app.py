from flask import Flask
from flask import send_file

app = Flask(__name__)
@app.route("/1")
def send1():
    return send_file("./C2_1")

@app.route("/2")
def send2():
    return send_file("./C2_2")

@app.route("/3")
def send3():
    return send_file("./C2_3")


if __name__ == "__main__":
    app.run(host='0.0.0.0')