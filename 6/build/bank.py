#!/usr/bin/env python3
from ArgumentParser import ArgumentParser
from sys import stderr, argv
from socketserver import TCPServer, StreamRequestHandler
import sys
from os import path, urandom
import json
import socket
from helpers import print_dict, getHashedMessage, validPort, validFileName, isSameHash
import signal
import threading

def main():
    args = parseArguments()
    ip = 'localhost'

    validPort(args.port)
    port = args.port
    validFileName(args.authFile)

    with Bank((ip, port), Handler, authFile=args.authFile) as bank:
        ExitHandler(bank)
        bank.serve_forever()


def parseArguments():
    parser = ArgumentParser()
    parser.add_argument('-p', default=3000, help="The port used for "
                        "the bank. If argument is not given, port " +
                        "%d is used." % 3000,
                        dest='port', type=int)
    parser.add_argument('-s', default='bank.auth', help="File name of " +
                        "the auth file. If argument is not given, " +
                        "'%s' is used." % 'bank.auth',
                        dest="authFile")
    return parser.parse_args(argv[1:])


class Bank(TCPServer):
    '''
    The bank class
    '''
    accounts = {}
    lastAction = {}
    lastAccounts = {}

    def __init__(self, server_address, Handler, authFile='bank.auth'):
        '''
        Creates a new bank
        ip:         the ip of the bank. Usually it should be localhost
        port:       the port on which the bank is listening
        authFile:   the auth-File the bank will create.
        '''
        TCPServer.__init__(self, server_address, Handler)
        TCPServer.allow_reuse_address = True
        self.createAuthToken(filename=authFile)

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.server_close()

    def createAuthToken(self, filename='bank.auth', key_length=32):
        '''
        Creates the auth file if it is not already exisitingself.
        filename:   name of the new auth file
        key_length: length of the key stored in the file in bit

        Returns:    Prints 'created' on stdout if the file could be created
                    successfully. Else exits the program with exit code 255.
        '''
        if path.exists(filename):
            sys.exit(255)
        else:
            global key
            with open(filename, 'wb') as f:
                new_key = urandom(32)
                key = new_key
                f.write(bytes(new_key))

        print("created", flush=True, file=sys.stdout)

    @staticmethod
    def addAccount(dict):
        account = dict['account']
        balance = dict['initial_balance']

        if account in Bank.accounts.keys():
            return 0
        Bank.accounts.update({account: balance})
        return 1

    @staticmethod
    def deposit(dict):
        account = dict['account']
        if account not in Bank.accounts.keys():
            return 0

        Bank.accounts[account] = Bank.accounts[account] + dict['deposit']
        return 1

    @staticmethod
    def withdraw(dict):
        account = dict['account']
        if account not in Bank.accounts.keys():
            return 0
        value = Bank.accounts[account] - dict['withdraw']
        if value < 0:
            return 0
        Bank.accounts[account] = value
        return 1

    @staticmethod
    def get(dict):
        account = dict['account']
        if account not in Bank.accounts.keys():
            return False
        return {account: Bank.accounts[account]}

    @staticmethod
    def confirm():
        if not Bank.lastAction == {}:
            print_dict(Bank.lastAction)
            Bank.lastAction = {}
            Bank.lastAccounts = {}


    @staticmethod
    def backupAccounts():
        Bank.lastAccounts = dict(Bank.accounts)


class Handler(StreamRequestHandler):
    '''
    Handles the messages which arrives at the socket
    '''
    timeout = 1
    timeoutT = None
    lock = threading.Lock()
    ack_arrived = False
    timeout = 10


    def handle(self):
        self.data = None
        try:
            self.data = self.request.recv(1024).strip()
        except socket.timeout:
            print("protocol_error handle1", file=stderr)
            print("protocol_error", flush=True)
            return

        try:
            dict = json.loads(self.data.decode("utf-8"))
        except:
            print("protocol_error handle2", file=stderr)
            print('protocol_error', flush=True)
            return

        tmp = {}
        tmp.update(dict)
        if isSameHash(key, tmp):
            sender = dict.pop('sender')
            if sender != "atm":
                print("protocol_error handle3", file=stderr)
                print("protocol_error", flush=True)
                return

            dict.pop('hash')
            mode = dict['mode']
            dict.pop('mode')

            returnType = -1
            if mode == 'ack':
                returnType = -2
                print_dict(Bank.lastAction)
                Bank.lastAction = {}

            else:
                Bank.backupAccounts()
                if mode == "new":
                    returnType = Bank.addAccount(dict)
                    if returnType:
                        dict['initial_balance'] = dict['initial_balance']
                elif mode == "deposit":
                    returnType = Bank.deposit(dict)
                    if returnType:
                        dict['deposit'] = dict['deposit']
                elif mode == 'withdraw':
                    returnType = Bank.withdraw(dict)
                    if returnType:
                        dict['withdraw'] = dict['withdraw']
                elif mode == 'get':
                    msg = Bank.get(dict)
                    if not msg:
                        returnType = 0
                    else:
                        dict['balance'] = msg[dict['account']]
                        returnType = 2

                if returnType == -1:
                    print("protocol_error handle4", file=stderr)
                    print("protocol_error", flush=True)
                elif returnType != -2:
                    if returnType == 1:
                        msg = getHashedMessage(key=key, mode="res", sender="bank", dict={"code": True})
                    elif returnType == 0:
                        msg = getHashedMessage(key=key, mode="res", sender="bank", dict={"code": False})
                    elif returnType == 2:
                        msg = getHashedMessage(key=key, mode="res", sender="bank", dict={"code": True, "balance": msg[dict['account']]})
                    self.wfile.write(bytes(json.dumps(msg, sort_keys=True), "utf-8"))
                    if returnType > 0:
                        Bank.lastAction = dict
        else:
            print("protocol_error handle5", file=stderr)
            print("protocol_error", flush=True)


# https://stackoverflow.com/questions/18499497/how-to-process-sigterm-signal-gracefully
class ExitHandler:
    kill_now = False

    def __init__(self, server):
        self.server = server
        signal.signal(signal.SIGINT, self.exit_gracefully)
        signal.signal(signal.SIGTERM, self.exit_gracefully)



    def exit_gracefully(self, signum, frame):
        shutdown = threading.Thread(target=self.exitServer)
        shutdown.start()


    def exitServer(self):
        self.server.shutdown()
        sys.exit(0)


if __name__ == '__main__':
    main()
