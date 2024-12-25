from http.server import HTTPServer, BaseHTTPRequestHandler  # класс
import qrcode
import random
import socket
import logging
import io
import qrcode
from io import BytesIO  # класс
import ssl

logging.basicConfig(level=logging.INFO, filename="py_log.log", filemode="w",
                    format="%(asctime)s %(levelname)s %(message)s")


def random_token():
    str1 = '123456789'
    str2 = 'qwertyuiopasdfghjklzxcvbnm'
    str3 = str2.upper()
    str4 = str1 + str2 + str3
    ls = list(str4)
    random.shuffle(ls)
    psw = ''.join([random.choice(ls) for x in range(4)])
    return psw


toclientmessage = ''
frommobile = b''  # Embedded
token = str(random_token())


def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    return s.getsockname()[0]


class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):

    # определяем метод `do_GET`
    def do_GET(self):
        global toclientmessage, frommobile
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        User_Agent = str(self.headers['User-Agent'])
        User_Token = str(self.headers['token'])
        if (User_Agent == 'mobile'):
            if (token == User_Token):
                # print(toclientmessage)
                print("receive get from mobile")
                if (toclientmessage == ''):
                    self.wfile.write(str.encode("empty"))
                else:
                    self.wfile.write(str.encode("w"))
                    logging.info("Mobile User Request")
                    toclientmessage = ''
            else:
                print("receive get from mobile with unknown token")
                self.send_response(403)
                self.end_headers()
        elif (User_Agent == 'Embedded'):
            # if(frommobile)
            print("receive get from Embedded")
            print(frommobile)
            self.wfile.write(frommobile)  # запрос включения/выключения от устройства
            frommobile = b''


        else:
            logging.info("Unknown User")
            self.send_response(400)
            self.end_headers()

    # определяем метод `do_POST`
    def do_POST(self):
        global toclientmessage, frommobile
        content_length = int(self.headers['Content-Length'])
        User_Agent = str(self.headers['User-Agent'])
        User_Token = str(self.headers['token'])

        if (User_Agent == 'mobile'):
            if (token == User_Token):
                print("receive post from mobile")
                frommobile = self.rfile.read(content_length)  # включение/выключение
                logging.info(f"Token valide. Mobile User {frommobile} device")
                self.send_response(200)
                self.end_headers()
            else:
                logging.info("Try connect. Token not valide")
                print("receive post from mobile with unknown token")
                self.send_response(403)
                self.end_headers()
        elif (User_Agent == 'Embedded'):
            print("receive post from Embedded")
            body = self.rfile.read(content_length)
            self.send_response(200)
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            self.wfile.write(str.encode(""))
            toclientmessage = "notnull"
            logging.info("Alarm Signal")
            print(body)
        else:
            logging.info("Unknown User")
            self.send_response(400)
            self.end_headers()


data = "https://" + str(get_local_ip()) + ":8080-" + token
qr = qrcode.QRCode()
qr.add_data(data)
f = io.StringIO()
qr.print_ascii(out=f)
f.seek(0)
print(f.read())
print(get_local_ip())
print(token)
httpd = HTTPServer(('', 8080), SimpleHTTPRequestHandler)
ssl_context = ssl.SSLContext(protocol=ssl.PROTOCOL_TLS_SERVER)
ssl_context.load_cert_chain('./app.crt', keyfile='./app.key')
httpd.socket = ssl_context.wrap_socket(httpd.socket, server_side=True)
httpd.serve_forever()