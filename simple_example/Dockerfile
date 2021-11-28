FROM python:3.8

WORKDIR /usr/src/app

COPY . .

RUN pip install pwn pycryptodome==3.4.3

CMD ["python3", "server.py"]