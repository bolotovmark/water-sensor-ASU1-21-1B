from http.server import HTTPServer, BaseHTTPRequestHandler
from io import BytesIO

class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):
    toclientmessage = ''
    # определяем метод `do_GET`
    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers
        print(self.toclientmessage)
        if (self.toclientmessage == ''):
            self.wfile.write(str.encode('no'))
        else:
            self.wfile.write(str.encode(self.toclientmessage))
            toclientmessage = ''
    # определяем метод `do_POST`
    def do_POST(self):
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
        print(body)
httpd = HTTPServer(('', 8080), SimpleHTTPRequestHandler)
httpd.serve_forever()