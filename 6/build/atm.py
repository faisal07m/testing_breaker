#!/usr/bin/env python3

from ArgumentParser import ArgumentParser
from socket import socket, AF_INET, SOCK_STREAM, timeout, SO_REUSEADDR, SOL_SOCKET
from helpers import isSameHash, print_dict, validPort, validAccountName, validFileName, getHashedMessage
from sys import exit
from os import path
import sys
import json


def main():
    args = parseArguments()
    if args.port.startswith("0"):
        sys.exit(255)
    tmpport = int(args.port, 10)

    validPort(tmpport)

    global port
    global ip
    port = tmpport

    testIP = args.ip.split('.')
    if len(testIP) != 4:
        sys.exit(255)
    for num in testIP:
        if int(num) < 0 or int(num) > 255:
            sys.exit(255)
        if len(num) > 3:
            sys.exit(255)


    ip = args.ip

    global card
    if args.cardFile:
        card = args.cardFile
    else:
        card = args.account + ".card"

    validFileName(card)
    authfile = 'bank.auth'
    if args.authFile:
        authfile = args.authFile

    validFileName(authfile)

    validAccountName(args.account)

    global key
    with open(authfile, "rb") as auth:
        key = auth.read()
    validAccountName(args.account)

    if(args.n):
        createNewAccount(account=args.account, initial_balance=args.n)

    if(args.d):
        deposit(account=args.account, cardFile=card, amount=args.d)

    if(args.w):
        withdraw(account=args.account, cardFile=card, amount=args.w)

    if args.g:
        get(account=args.account, cardFile=card)


def createNewAccount(account, initial_balance):
    '''
    Creates a new Account
    account:    The account name
    card:       The card-File linked to the account
    initial_balance:    The initial_balance of the account
    '''
    # balance = convertInputToInt(initial_balance)
    # balance now counted in cents (1 *100)
    balance = initial_balance
    balance = convertInputToInt(balance)
    if balance < 1000:
        exit(255)

    if path.exists(card):
        exit(255)

    dict = {"account": account, "initial_balance": balance}
    sendMessage(dict, "new")

def sendAck():
    with socket(family=AF_INET,
                type=SOCK_STREAM) as sock2:
        sock2.setsockopt(SOL_SOCKET, SO_REUSEADDR, 1)
        sock2.connect((ip, port))
        msg = getHashedMessage(key, "ack", sender="atm")
        sock2.sendall(bytes(json.dumps(msg, sort_keys=True), "utf-8"))

def deposit(account, cardFile, amount):
    '''Used to deposit money to an account
    account:    The account which shall store the money
    card:       The card associated with the account.
    amount:     The amount to be stored
    '''
    amount = convertInputToInt(amount)
    if amount <= 0:
        sys.exit(255)

    dict = {"account": account}
    cardAccount = None
    try:
        with open(cardFile, "r") as card:
            cardAccount = card.readline()

    except OSError:
        sys.exit(255)

    if cardAccount != account:
        sys.exit(255)

    dict.update({"deposit": amount})
    sendMessage(dict, "deposit")


def withdraw(account, cardFile, amount):
    '''Used to deposit money to an account
    account:    The account which shall store the money
    card:       The card associated with the account.
    amount:     The amount to be stored
    '''
    amount = convertInputToInt(amount)
    if amount <= 0:
        sys.exit(255)

    dict = {"account": account}
    cardAccount = None
    try:
        with open(cardFile, "r") as card:
            cardAccount = card.readline()

    except OSError:
        sys.exit(255)

    if cardAccount != account:
        sys.exit(255)

    dict.update({"withdraw": amount})
    sendMessage(dict, "withdraw")


def get(account, cardFile):

    dict = {"account": account}
    cardAccount = None
    try:
        with open(cardFile, "r") as card:
            cardAccount = card.readline()

    except OSError:
        sys.exit(255)

    if cardAccount != account:
        sys.exit(255)

    sendMessage(dict, "get")


def sendMessage(dict, mode):

    msg = getHashedMessage(key, mode, "atm", dict)

    with socket(family=AF_INET, type=SOCK_STREAM) as sock:
        sock.settimeout(10)
        sock.setsockopt(SOL_SOCKET, SO_REUSEADDR, 1)
        try:
            sock.connect((ip, port))

            msg = getHashedMessage(key, mode, "atm", dict)
            sock.sendall(bytes(json.dumps(msg, sort_keys=True), "utf-8"))
            received = sock.recv(1024).strip()

            try:
                response = json.loads(received.decode("utf-8"))
            except:
                sys.exit(63)

            tmp = {}
            tmp.update(response)
            if isSameHash(key, tmp):

                sender = response["sender"]

                if sender != "bank":
                    exit(63)
                if response["code"] == True:
                    if mode == "new":
                        print_dict(dict)
                        with open(card, "w") as pin:
                            pin.write(dict["account"])
                            sendAck()
                        exit(0)
                    elif mode == "get":
                        sendAck()
                        tmp = {"balance": response["balance"], "account":dict["account"]}
                        print_dict(tmp)
                    else:
                        sendAck()
                        dict[mode]=dict[mode]
                        print_dict(dict)
                elif received == 'False':
                    exit(255)
                else:
                    exit(255)

            else:
                exit(63)
        except timeout as to:
            exit(63)



def convertInputToInt(string_amount):
    try:
        number = float(string_amount)
    except ValueError:
        sys.exit(255)

    if number <= 0 or number > 4294967295.99 or (string_amount[0] == '0' and
                                                 not string_amount[1] == '.'):
        sys.exit(255)
    if string_amount.find('.') == -1:
        sys.exit(255)
        return int(string_amount + "00")
    elif string_amount.find('.') == 0:
        sys.exit(255)
    else:
        # find gives index BEFORE .
        cents_pos = len(string_amount) - string_amount.find('.') - 1
        if cents_pos == 1:
            sys.exit(255)
        elif cents_pos == 2:
            return int(string_amount.replace('.', ''))
        else:
            sys.exit(255)


def parseArguments():
    # TODO remove argparse to use the modified argumentParser
    # TODO Account must be unique
    '''
    Parses the arguments of the atmself.
    A Objects is return in which the paramters values are callabel
    '''

    parser = ArgumentParser(conflict_handler='error')

    parser.add_argument("-s", help="""The authentication file that bank creates for the atm.
        If -s is not specified, the default filename is "bank.auth"
        (in the current working directory).
        If the specified file cannot be opened or is invalid,
        the atm exits with a return code of 255.""", dest="authFile",
                        default="bank.auth", metavar="auth_file")

    parser.add_argument("-i", help="""The IP address that bank is running on.
        The default value is "127.0.0.1".""", dest="ip", default="127.0.0.1",
                        metavar="ip-address")

    parser.add_argument("-p", help="""The TCP port that bank is listening on.
                        The default is 3000.""",
                        dest="port", default="3000")

    parser.add_argument("-c", help="""The customer's atm card file.
    The default value is the account name prepended to ".card"
    ("<account>.card"). For example, if the account name was 55555,
    the default card file is "55555.card".""",
                        dest="cardFile", metavar="CARD-FILE")

    parser.add_argument("-a", help="""The customer's account name. """,
                        dest="account", required=False)

    modesParser = parser.add_mutually_exclusive_group()
    modesParser.add_argument("-n", help=""" Create a new account with the given balance.
        The account must be unique (ie, the account must not already exist).
        The balance must be greater than or equal to 10.00. """)

    modesParser.add_argument("-d", help="""Deposit the amount of money specified.
        The amount must be greater than 0.00. The specified account must exist,
        and the card file must be associated with the given account
        (i.e., it must be the same file produced by atm when the account
        was created).""")

    modesParser.add_argument("-w", help="""Withdraw the amount of money specified.
        The amount must be greater than 0.00, and the remaining balance must be
        nonnegative. The card file must be associated with the specified
        account (i.e., it must be the same file produced by atm when the
        account was created).""")

    modesParser.add_argument("-g", help="""Get the current balance of the account.
        The specified account must exist, and the card file must be associated
        with the account.""", action="store_true")

    return parser.parse_args(sys.argv[1:])


if __name__ == '__main__':
    main()
