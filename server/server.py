from http.server import HTTPServer, BaseHTTPRequestHandler  # класс
import qrcode
import random
import socket
import string
import logging
import io
import qrcode
from io import BytesIO  # класс
import ssl


def random_token():
    DIGITS = '123456789'
    ls = list(string.ascii_letters)
    for i in DIGITS:
        ls.append(i)
    psw = ''.join([random.choice(ls) for x in range(4)])
    return psw


def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    return s.getsockname()[0]


class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):
    __logger = None
    __token = None
    __to_client_message = ''
    __from_mobile = b''  # Embedded

    @classmethod
    def set_logger(cls, logger):
        cls.__logger = logger

    @classmethod
    def set_token(cls, token):
        cls.__token = token

    def log_message(self, format, *args):
        logger = SimpleHTTPRequestHandler.__logger
        logger.info(''.join(args))
        return

    # определяем метод `do_GET`
    def do_GET(self):
        logger = SimpleHTTPRequestHandler.__logger
        token = SimpleHTTPRequestHandler.__token
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        user_agent = str(self.headers['User-Agent'])
        user_token = str(self.headers['token'])
        if (user_agent == 'mobile'):
            if (token == user_token):
                logger.info("receive get from mobile")
                if (SimpleHTTPRequestHandler.__to_client_message == ''):
                    self.wfile.write(str.encode("empty"))
                else:
                    self.wfile.write(str.encode("w"))
                    logger.info("Mobile User Request")
                    SimpleHTTPRequestHandler.__to_client_message = ''
            else:
                logger.info("receive get from mobile with unknown token")
                self.send_response(403)
                self.end_headers()
        elif (user_agent == 'Embedded'):
            logger.info("Unknown User")
            self.wfile.write(SimpleHTTPRequestHandler.__from_mobile)  # запрос включения/выключения от устройства
            SimpleHTTPRequestHandler.__from_mobile = b''
        else:
            logging.info("Unknown User")
            self.send_response(400)
            self.end_headers()

    # определяем метод `do_POST`
    def do_POST(self):
        logger = SimpleHTTPRequestHandler.__logger
        token = SimpleHTTPRequestHandler.__token
        content_length = int(self.headers['Content-Length'])
        user_agent = str(self.headers['User-Agent'])
        user_token = str(self.headers['token'])
        if (user_agent == 'mobile'):
            if (token == user_token):
                SimpleHTTPRequestHandler.__from_mobile = self.rfile.read(content_length)  # включение/выключение
                logger.info(f"Receive post from mobile. Token valide. Mobile User {SimpleHTTPRequestHandler.__from_mobile} device")
                self.send_response(200)
                self.end_headers()
            else:
                logging.info("Try connect. Token not valide. Receive post\
                                              from mobile with unknown token")
                self.send_response(403)
                self.end_headers()
        elif (user_agent == 'Embedded'):
            body = self.rfile.read(content_length)
            self.send_response(200)
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            self.wfile.write(str.encode(""))
            SimpleHTTPRequestHandler.__to_client_message = "notnull"
            logging.info("Receive post from Embedded. Alarm Signal")
        else:
            logging.info("Unknown User")
            self.send_response(400)
            self.end_headers()

def main():
    logger = logging.getLogger('logger')
    logging.basicConfig(level=logging.INFO, filename="py_log.log",filemode="w",
                    format="%(asctime)s %(levelname)s %(message)s")
    ch = logging.StreamHandler()
    ch.setLevel(logging.DEBUG)
    formatter = logging.Formatter("%(levelname)s %(message)s")
    ch.setFormatter(formatter)
    logger.addHandler(ch)

    token = str(random_token())
    data = f"https://{str(get_local_ip())}:8080-{token}"
    qr = qrcode.QRCode()
    qr.add_data(data)
    f = io.StringIO()
    qr.print_ascii(out=f)
    f.seek(0)
    logger.info(f.read())
    logger.info(get_local_ip())
    logger.info(token)
    SimpleHTTPRequestHandler.set_logger(logger)
    SimpleHTTPRequestHandler.set_token(token)
    httpd = HTTPServer(('', 8080), SimpleHTTPRequestHandler)
    ssl_context = ssl.SSLContext(protocol=ssl.PROTOCOL_TLS_SERVER)
    ssl_context.load_cert_chain('./app.crt',keyfile='./app.key')
    httpd.socket = ssl_context.wrap_socket (httpd.socket, server_side=True)
    httpd.serve_forever()

if __name__ == "__main__":
    main()