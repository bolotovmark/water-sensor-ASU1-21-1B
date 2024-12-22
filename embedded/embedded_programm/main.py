#!/usr/bin/env python3
import OPi.GPIO as GPIO
import requests
import time
if __name__ == "__main__":
    detecter_channel = 12
    sound_channel = 18

    GPIO.setmode(GPIO.BOARD)
    GPIO.setup(detecter_channel, GPIO.IN)
    GPIO.setup(sound_channel, GPIO.OUT)
    time.sleep(3)

    while True:
        rise = GPIO.wait_for_edge(detecter_channel, GPIO.RISING, 10000)
        send = False
        while not send:
            response = requests.post("http://192.168.43.42:8080", data={"signal": "alarm"})
            GPIO.output(sound_channel, 1)
            if response.status_code == 200:
                send = True
                print("Message_delevered.")
            else:
                print("Error! Server status code: " + response.status_code)
        time.sleep(4)
        GPIO.output(sound_channel, 0)


    GPIO.cleanup(detecter_channel)
    GPIO.cleanup(sound_channel)