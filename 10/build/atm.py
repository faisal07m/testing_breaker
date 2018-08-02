#!/usr/bin/python3

import sys
import getopt
import re
import socket
import json
import collections
import os
from os import path
import base64
from Crypto.Cipher import AES
from Crypto.Hash import SHA256

#Test the infrastructure build again using empty commit#
ERROR_CODE = 255
socket.setdefaulttimeout(10)
class atm:
    def __init__(self):
        self.auth_file = "bank.auth"
        self.ip_address = "127.0.0.1"
        self.port = 3000
        self.card_file = None
        self.account = None
        self.command = None
        self.commandArg = None
        self.output = {}
        self.card_file_path = ""

    # some validation of commmand line arguments and setting values for the right ones
    def getParameters(self, argv):
        if(self.has_duplicates(argv)):
            self.exitProgram()
        count=0
        for i in range(0, len(argv)):
            if(len(argv[i]) > 4096):
                self.exitProgram()
            if(argv[i] in ["-n", "-d", "-w", "-g" , "-gs","-gi", "-gp", "-gc", "-ga"]):
                count=count+1
            if argv[i] in ["-s", "-i", "-p", "-c", "-a", "-n", "-d", "-w"]:
                if (i + 1 == len(argv)):
                    self.exitProgram()
                if (str(argv[i+1]).startswith("-")):
                    self.exitProgram()
            if argv[i] in ["-g"]:
                if i+1 !=len(argv):
                    if str(argv[i+1]).startswith("-") is False:
                        self.exitProgram()
            if (argv[i] in ["-gs","-gi", "-gp", "-gc", "-ga"]):
                if (i + 1 == len(argv)):
                    self.exitProgram()
                if(str(argv[i+1]).startswith("-")):
                    self.exitProgram()
                self.command = 'get_balance'
            if (str(argv[i]).startswith("-") and str(argv[i])[:2] not in ["-s", "-i", "-p", "-c", "-a",
                                                                                       "-n", "-d", "-w", "-g"]):
                self.exitProgram()
            if (str(argv[i]).startswith("-") is False) and (str(argv[i - 1]).startswith("-") is False):
                self.exitProgram()
        if count !=1 :
            self.exitProgram()

        opts, args = getopt.getopt(argv, "s:i:p:c:a:n:d:w:g")
        for opt, arg in opts:
            if opt in ["-s","-gs"]:
                self.auth_file = arg
            if opt in ["-i","-gi"]:
                self.ip_address = arg
            if opt in ["-p","-gp"]:
                self.port = arg
            if opt in ["-c","-gc"]:
                self.card_file = arg
            if opt in ["-a","-ga"]:
                self.account = arg
                if (self.card_file is None):
                    self.card_file = self.account + ".card"
            if opt in ["-n"]:
                self.command = 'new_account'
                self.commandArg = arg
            if opt in ["-d"]:
                self.command = 'deposit'
                self.commandArg = arg
            if opt in ["-w"]:
                self.command = 'withdraw'
                self.commandArg = arg
            if opt in ["-g"]:
                self.command = 'get_balance'

    #some more validation of command line arguments
    def validateParameters(self):

        if self.account is None or self.command is None:
            self.exitProgram()


        # auth_file name validation
        if (self.auth_file == "." or self.auth_file == ".."):
            self.exitProgram()
        if (self.isWithinLength(len(self.auth_file), 1, 127) is False):
            self.exitProgram()

        if (self.isAccordingToExpr("^[_\-\.0-9a-z]+$", self.auth_file) is False):
            self.exitProgram()

        # IPv4 validation
        if (self.isAccordingToExpr(
                r"^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$",
                self.ip_address) is False):
            self.exitProgram()

        # Port validation
        if ((self.isAccordingToExpr(r"^[1-9][0-9]*$", str(self.port)) is False) or (int(self.port) < 1024) or (
                int(self.port) > 65535)):
            self.exitProgram()

        # Card File Validation
        if (self.card_file != None):
            if (self.card_file == "." or self.card_file == ".."):
                self.exitProgram()
            if (self.isWithinLength(len(self.card_file), 1, 127) is False):
                self.exitProgram()
            if (self.isAccordingToExpr("^[_\-\.0-9a-z]+$", self.card_file) is False):
                self.exitProgram()

        if self.command == "new_account":
            self.card_file_path = os.getcwd()
            self.card_file_path = os.path.join(self.card_file_path, self.card_file)

            if os.path.exists(self.card_file_path):
                self.exitProgram()

        # Account name validation

        if (self.isWithinLength(len(self.account), 1, 122) is False):
            self.exitProgram()
        if (self.isAccordingToExpr("^[_\-\.0-9a-z]+$", self.account) is False):
            self.exitProgram()

        # Currency Amount Validation

        if ((self.command != None) and (self.command != "get_balance")):
            if (self.isAccordingToExpr("^(0|[1-9][0-9]*)\.[0-9]{2}$", self.commandArg) is False):

                self.exitProgram()
            if (float(self.commandArg) < 0.01 or float(self.commandArg) > 4294967295.99):
                self.exitProgram()
            if (self.command == "new_account"):
                if (float(self.commandArg) < 10.00):
                    self.exitProgram()

    def isWithinLength(self, length, lowLim, upLim):
        if (length >= lowLim and length <= upLim):
            return True
        else:
            return False

    def isAccordingToExpr(self, expression, input):
        if (re.match(expression, input) is None):
            return False
        else:
            return True

    def exitProgram(self):
        sys.exit(ERROR_CODE)

    def has_duplicates(self,list_of_values):
        value_dict = collections.defaultdict(int)
        for item in list_of_values:
            if(item in ["-s", "-i", "-p", "-c", "-a", "-n", "-d", "-w", "-g"]):
                value_dict[item] += 1
        return any(val > 1 for val in value_dict.values())

    def splitArgs(self, argv):
        b=[]
        for arg in argv:
            if str(arg).startswith("-") and len(arg) > 2 and arg not in ["-gs","-gi", "-gp", "-gc", "-ga"]:
                b.append(arg[:2])
                b.append(arg[2:])
            else:
                b.append(arg)
        return b

    def handleRequest(self):
        client = Client(self.auth_file)
        request_json = self.createRequestJson()

        # send request
        try:
            client.connect(self.ip_address, self.port)
            client.send(request_json)

            response = client.recv()
            filteredResponse = self.filterResponse(response, self.command)
            client.close()

            print(filteredResponse, flush=True)
            sys.exit(0)

        except socket.timeout:
            exit(63)
        except socket.error:
            exit(63)
        return

    def createRequestJson(self):
        requestJson = {}
        requestJson["account"] = self.account

        if self.command != "new_account":
            content = self.getCardFileData()
            if content == "":
                self.exitProgram()
            requestJson["pin"] = content

        requestJson["command"] = self.command
        requestJson["command_args"] = self.commandArg
        return requestJson

    def filterResponse(self, response, command):
        if 'error' in response:
            self.exitProgram()

        filteredRes = ""
        if command == 'new_account':
            filteredRes = '{"initial_balance": '
            filteredRes += str(response["initial_balance"]) + ', '
            filteredRes += '"account": "'
            filteredRes += response["account"] + '"}'
            self.createCardFile(response["pin"])
            return filteredRes

        filteredRes = '{"account": "'

        filteredRes += response["account"] + '", '

        if command == 'deposit':
            filteredRes += '"deposit": '
            filteredRes += str(response["deposit"]) + '}'
        if command == 'get_balance':
            filteredRes += '"balance": '
            filteredRes += str(response["balance"]) + '}'
        if command == 'withdraw':
            filteredRes += '"withdraw": '
            filteredRes += str(response["withdraw"]) + '}'

        return filteredRes

    def createCardFile(self, data):
        f = open(self.card_file_path,'w')
        f.write(data)
        f.close()
        return

    def getCardFileData(self):
        try:
            myPath = os.getcwd()
            mypath = os.path.join(myPath, self.card_file)
            f = open(mypath, 'r')
            content = f.read()
            f.close()
        except FileNotFoundError:
            content = ""
        # finally:
        #     f.close()
        return content

class Client(object):
    socket = None
    def __init__(self, authFile):
        self.authFile = authFile

    def __del__(self):
        self.close()

    def connect(self, host, port):
        self.socket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
        self.socket.settimeout(10)
        try:
            self.socket.connect((host, int(port)))
        except socket.timeout:
            exit(63)
        except socket.error:
            exit(63)

        return self

    def send(self, data):
        _send(self.socket, data, self.authFile)
        return self

    def recv(self):
        return _recv(self.socket, self.authFile)


    def close(self):
        if self.socket:
            self.socket.close()
            self.socket = None

def getKey(authFile):
    myPath = os.getcwd()
    myPath = os.path.join(myPath,authFile)
    key = b""
    if path.isfile(myPath) and path.isfile(myPath):
        try:
            with open(authFile, 'rb') as auth:
                key = auth.read()
        except EnvironmentError:
            exit(255)
    else:
        exit(255)
    return key

def _send(sock, data, authFileName):
    try:
        serialized = json.dumps(data)
        key = getKey(authFileName)
        encrypted_data = encrypt(key, str.encode(serialized))

        # send the length of the serialized data first
        sock.send(('%d\n' % len(encrypted_data)).encode())
        # send the serialized data
        sock.sendall(encrypted_data.encode())
    except socket.timeout:
        exit(63)
    except socket.error:
        exit(63)

def _recv(sock, authFileName):
    deserialized = {}
    try:
        sock.settimeout(10)

        # read the length of the data, letter by letter until we reach EOL
        length_str = ''
        char = sock.recv(1).decode()
        if not char:
            exit(63)
        while char != '\n':
            length_str += char
            char = sock.recv(1).decode()
        total = int(length_str)

        received_data = sock.recv(total).decode()
        sock.settimeout(None)
        key = getKey(authFileName)
        decrypted = decrypt(key, received_data)
        deserialized = json.loads(decrypted.decode())

    except socket.timeout:
        exit(63)
    except socket.error:
        exit(63)

    return deserialized

## Ecncryption and Decryption algorithm
def encrypt(key, source, encode=True):
    key = SHA256.new(key).digest()
    IV = os.urandom(AES.block_size)
    encryptor = AES.new(key, AES.MODE_CBC, IV)
    padding = AES.block_size - len(source) % AES.block_size
    source += bytes([padding]) * padding
    data = IV + encryptor.encrypt(source)
    return base64.b64encode(data).decode("latin-1") if encode else data

def decrypt(key, source, decode=True):
    if decode:
        source = base64.b64decode(source.encode("latin-1"))
    key = SHA256.new(key).digest()
    IV = source[:AES.block_size]
    decryptor = AES.new(key, AES.MODE_CBC, IV)
    data = decryptor.decrypt(source[AES.block_size:])
    padding = data[-1]
    if data[-padding:] != bytes([padding]) * padding:
        raise ValueError("Invalid padding...")
    return data[:-padding]


if __name__ == "__main__":
    argv = sys.argv[1:]
    atm = atm()
    argv = atm.splitArgs(argv)
    atm.getParameters(argv)
    atm.validateParameters()
    atm.handleRequest()
