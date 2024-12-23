from http.server import HTTPServer, BaseHTTPRequestHandler
from io import BytesIO

toclientmessage=''
class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):
    # определяем метод `do_GET`
    def do_GET(self):
        global toclientmessage
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        print(toclientmessage)
        if toclientmessage == '':
            self.wfile.write(str.encode("empty"))
        else:
            self.wfile.write(str.encode("w"))
            toclientmessage = ''
    # определяем метод `do_POST`
    def do_POST(self):
        global toclientmessage
        content_length = int(self.headers['Content-Length'])
        body = self.rfile.read(content_length)
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        response = BytesIO()
        response.write(b'This is POST request. ')
        response.write(b'Received: ')
        response.write(body)
        self.wfile.write(response.getvalue())
        toclientmessage = "notnull"
        print(body)
httpd = HTTPServer(('', 8080), SimpleHTTPRequestHandler)
httpd.serve_forever()