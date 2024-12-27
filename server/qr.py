import qrcode
import random

import socket


def get_local_ip():
    hostname = socket.gethostname()
    local_ip = socket.gethostbyname(hostname)
    return local_ip


print(get_local_ip())


def random_token():
    str1 = '123456789'
    str2 = 'qwertyuiopasdfghjklzxcvbnm'
    str3 = str2.upper()
    str4 = str1 + str2 + str3
    ls = list(str4)
    random.shuffle(ls)
    psw = ''.join([random.choice(ls) for x in range(12)])
    return psw


# http://[ip]:8080-[token]
# пример данных
data = "https://" + str(get_local_ip()) + ":8080-" + str(random_token())
# имя конечного файла
filename = "site.png"
# генерируем qr-код
img = qrcode.make(data)
# сохраняем img в файл
img.save(filename)