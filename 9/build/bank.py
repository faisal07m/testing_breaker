#!/usr/bin/python3
import base64
import signal
import socket
import sys

import accounting
import arguments
import constants
import encryption
from Request import Request
from constants import BUFFER_SIZE
from myio import myprint, eprint


def input_error_exit():
    eprint("input error exit 255")
    sys.exit(255)


def process_request(request):
    """:returns True on success, False on failure"""
    if request.command == "n":
        return accounting.create_new_account(request)
    if request.command == "g":
        return accounting.check_balance(request)
    if request.command == "w":
        return accounting.withdraw(request)
    if request.command == "d":
        return accounting.deposit(request)
    eprint("Invalid command '" + request.command + "'. This shouldn't happen...")
    return False  # this should never happen


if __name__ == '__main__':

    args = arguments.read_bank_arguments(sys.argv[1:])
    if args is None:
        eprint("parsing of bank arguments failed, terminating...")
        input_error_exit()
    (port, auth_filename) = args
    if port is None:
        port = constants.DEFAULT_PORT
    if auth_filename is None:
        auth_filename = "bank.auth"

    key = encryption.create_random_key()
    eprint("bank key (base64): " + base64.b64encode(key).decode("utf-8"))
    auth_file = open(auth_filename, "wb")
    auth_file.write(key)
    auth_file.close()
    myprint("created")

    conn = None
    s = socket.socket()
    s.bind(("127.0.0.1", port))
    s.listen(1)


    def exit_gracefully(signum, frame):
        eprint("received SIGINT or SIGTERM, terminating gracefully...")
        if conn is not None:
            conn.close()
        s.close()
        sys.exit(0)


    signal.signal(signal.SIGINT, exit_gracefully)
    signal.signal(signal.SIGTERM, exit_gracefully)

    while True:
        conn, addr = s.accept()
        conn.settimeout(10)
        try:
            received_data = conn.recv(BUFFER_SIZE)
            eprint("bank received " + str(len(received_data)) + " bytes (encrypted)")
            if received_data:
                decrypted_data = encryption.decrypt_message(received_data, key)
                # eprint("bank received (decrypted): " + my_base64encode(received_data))
                if decrypted_data is None:
                    myprint("protocol_error")
                    conn.send(encryption.encrypt_message(constants.FAILURE, key))
                else:
                    eprint("processing message: " + decrypted_data)
                    success = True
                    request = Request.from_string(decrypted_data)
                    if request is None:
                        success = False
                    else:
                        success = process_request(request)
                    if success:
                        eprint("processing message -> SUCCESS")
                        json = request.json_str()
                        myprint(json)
                        conn.send(encryption.encrypt_message(json, key))
                    else:
                        eprint("processing message -> FAILURE")
                        myprint("protocol_error")
                        conn.send(encryption.encrypt_message(constants.FAILURE, key))
        except Exception as e:
            eprint(e)
            myprint("protocol_error")
        conn.close()
