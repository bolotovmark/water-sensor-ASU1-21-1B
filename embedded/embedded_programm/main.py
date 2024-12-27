#!/usr/bin/env python3
import OPi.GPIO as GPIO
import requests
import time
import logging

from threading import Thread, Event


def ask_server(logger, alarm_working):
    headers = requests.utils.default_headers()
    headers.update(
        {
            'User-Agent': 'Embedded'
        }
    )

    while True:
        try:
            response = requests.get("https://serverapp:8080",
                                    headers=headers,
                                    verify="app.crt")
            try:
                response.rise_for_status()
                if response.text == "on":
                    alarm_working.set()
                    logger.info("Alarm state is on.")
                elif response.text == "off":
                    alarm_working.clear()
                    logger.info("Alarm state is off.")
                else:
                    if not response.text == "":
                        logger.error("Wrong answer from server then asking state.")
            except requests.HTTPError as http_error:
                logger.error("Server status code: {http_error}")
        except requests.Timeout:
            logger.error("Server connection timeout.")
        except requests.ConnectionError:
            logger.error("Server connection error.")
        except requests.RequestException as error:
            logger.error(f"Error occured while server connection: {error}.")
        time.sleep(4)


def watch_alarm(logger, alarm_working):
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
        if alarm_working.is_set():
            rise = GPIO.wait_for_edge(detecter_channel, GPIO.RISING, 10000)
            logger.info("Alarm, catch signal from detector.")
            send = False
            while not send:
                try:
                    response = requests.post("https://serverapp:8080",
                                             data={"signal": "alarm"},
                                             headers=headers,
                                             verify="app.crt")
                    try:
                        response.rise_for_status()
                        send = True
                        logger.info("Message delivered to server.")
                    except requests.HTTPError as http_error:
                        logger.error("Server status code: {http_error}")
                except requests.Timeout:
                    logger.error("Server connection timeout.")
                except requests.ConnectionError:
                    logger.error("Server connection error.")
                except requests.RequestException as error:
                    logger.error(f"Error occured while server connection: {error}.")
                GPIO.output(sound_channel, 1)
        time.sleep(4)
        GPIO.output(sound_channel, 0)

    GPIO.cleanup(detecter_channel)
    GPIO.cleanup(sound_channel)


def main():
    logger = logging.getLogger('python_alarm')
    logging.basicConfig(level=logging.INFO,
                        filename="python_alarm_log.log",
                        filemode="w")

    ch = logging.StreamHandler()
    ch.setLevel(logging.DEBUG)
    formatter = logging.Formatter("%(levelname)s %(message)s")
    ch.setFormatter(formatter)
    logger.addHandler(ch)

    alarm_working = Event()

    ask_server_thread = Thread(target=ask_server,
                               args=(logger, alarm_working))
    watch_alarm_thread = Thread(target=watch_alarm,
                                args=(logger, alarm_working))

    ask_server_thread.start()
    watch_alarm_thread.start()

    ask_server_thread.join()
    watch_alarm_thread.join()


if __name__ == "__main__":
    main()
