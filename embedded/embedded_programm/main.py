import OPi.GPIO as GPIO
import requests
import time
if __name__ == "__main__":
    while True:
        detecter_channel = 12
        GPIO.setmode(GPIO.BOARD)
        GPIO.setup(detecter_channel, GPIO.IN)
        time.sleep(3)
        rise = GPIO.wait_for_edge(detecter_channel, GPIO.RISING, 10000)
        send = False
        while not send:
            response = requests.post("http://192.168.0.1", data={"signal":"alarm"})
            if response.status_code == 200:
                send = True
            else:
                print("Error! Server status code: " + response.status_code)
    GPIO.cleanup(detecter_channel)