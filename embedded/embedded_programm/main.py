#!/usr/bin/env python3
import OPi.GPIO as GPIO
import requests
import time
from threading import Thread


import urllib3
urllib3.disable_warnings(urllib3.exceptions.SecurityWarning)

alarm_working = True
max_retryes = 5


def ask_server():
    global alarm_working


    headers = requests.utils.default_headers()
    headers.update(
        {
            'User-Agent': 'Embedded'
        }
    )
    while True:
        response = requests.get("https://serverapp:8080", headers=headers, verify="app.crt")
        if response.text == "on":
            alarm_working = True
            print("Alarm state is on.")
        elif response.text == "off":
            alarm_working = False
            print("Alarm state is off.")
        else:
            print("Error! Wrong answer from server then asking for alarm state.")
    time.sleep(3)


def watch_alarm():
    headers = requests.utils.default_headers()
    headers.update(
            {
                'User-Agent': 'Embedded'
            }
        )
    detecter_channel = 12
    sound_channel = 18

    GPIO.setmode(GPIO.BOARD)
    GPIO.setup(detecter_channel, GPIO.IN)
    GPIO.setup(sound_channel, GPIO.OUT)
    time.sleep(3)

    while True:
        if alarm_working:
            rise = GPIO.wait_for_edge(detecter_channel, GPIO.RISING, 10000)
            print("Alarm, catch signal from detector.")
            send = False
            retryes = 0
            while not send and retryes < max_retryes:
                response = requests.post("https://serverapp:8080", data={"signal": "alarm"}, headers=headers,
                                         verify="app.crt")
                retryes += 1
                GPIO.output(sound_channel, 1)
                if response.status_code == 200:
                    send = True
                    print("Message delivered to server.")
                else:
                    print("Error! Server status code: " + response.status_code)
            time.sleep(4)
            GPIO.output(sound_channel, 0)

    GPIO.cleanup(detecter_channel)
    GPIO.cleanup(sound_channel)


def main():
    ask_server_thread = Thread(target=ask_server)
    watch_alarm_thread = Thread(target=watch_alarm)
    ask_server_thread.start()
    watch_alarm_thread.start()
    ask_server_thread.join()
    watch_alarm_thread.join()


if __name__ == "__main__":
    main()
