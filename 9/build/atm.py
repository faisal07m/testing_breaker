#!/usr/bin/python3
import base64
import os
import socket
import sys

import Request
import arguments
import constants
import encryption
from constants import BUFFER_SIZE
from myio import eprint, my_base64encode, myprint


def protocol_error_exit():
    eprint("protocol error exit 63")
    sys.exit(63)


def input_error_exit():
    eprint("input error exit 255")
    sys.exit(255)


def main():
    args = arguments.read_atm_arguments(sys.argv[1:])
    if args is None:
        eprint("parsing ATM arguments failed. terminating...")
        input_error_exit()
    (auth_filename, ip_address, port, card_filename, account_name, command, amount) = args
    if ip_address is None:
        ip_address = "127.0.0.1"
    if port is None:
        port = constants.DEFAULT_PORT
    if card_filename is None:
        card_filename = account_name + ".card"
    if auth_filename is None:
        auth_filename = "bank.auth"
    pin, key = None, None

    try:
        if command != "n":
            card_file = open(card_filename, 'r')
            pin = card_file.read()
            card_file.close()
        else:
            if os.path.isfile(card_filename):
                eprint("card file already exists:", card_filename)
                input_error_exit()
            pin = my_base64encode(encryption.create_random_key())
        auth_file = open(auth_filename, 'rb')
        key = auth_file.read()
        eprint("atm key (base64): " + base64.b64encode(key).decode("utf-8"))
        auth_file.close()
    except IOError as e:
        eprint(e)
        input_error_exit()

    request = Request.Request(account_name, pin, command, amount)

    s = socket.socket()
    s.settimeout(10)

    try:
        s.connect((ip_address, port))
        data_to_send = str(request)
        eprint("atm, sending (decrypted): " + data_to_send)
        encrypted_data_to_send = encryption.encrypt_message(data_to_send, key)
        eprint("atm, sending " + str(len(encrypted_data_to_send)) + " bytes, (encrypted)")
        s.send(encrypted_data_to_send)
        received_data = s.recv(BUFFER_SIZE)
        if not received_data:
            s.close()
            protocol_error_exit()
        decrypted_data = encryption.decrypt_message(received_data, key)
        if decrypted_data is None:
            s.close()
            protocol_error_exit()
        else:
            if decrypted_data == constants.FAILURE:
                eprint("received FAILURE code from bank. terminating...")
                s.close()
                input_error_exit()
            else:  # success
                eprint("received JSON result from bank")
                myprint(decrypted_data)
                if command == "n":
                    card_file = open(card_filename, 'w')
                    card_file.write(pin)
                    card_file.close()
    except Exception as e:
        eprint("socket error:", e)
        s.close()
        protocol_error_exit()


if __name__ == "__main__":
    main()
    sys.exit(0)
