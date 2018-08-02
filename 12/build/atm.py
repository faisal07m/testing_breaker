#!/usr/bin/env python
#coding: utf-8
import os
import json
import time
import sys
import argparse
import socket
import errno
import validator as val
from protocol import *


class ArgumentParser(argparse.ArgumentParser):
    def error(self, message):
        sys.stderr.write('Invalid arguments \n')
        sys.exit(255)

class ATM:
    def __init__(self, cardfile):
        self.keys = []
        self.cardFile = cardfile
        self.randomNum = os.urandom(10)
        # self.randomNum = "hshbasdbjkje" + self.cardFile
        #self.read_authfile(authFile)

    def getOperation(self, args):
        msg = {}
        if(args.initiate):
            msg = {'action':'n','account': args.account_name,'pinCode': self.randomNum,'money':args.initiate ,'timestamp': str(time.time())}
        if(args.deposit_money):
            msg = {'action':'d','account': args.account_name,'pinCode': self.randomNum,'money':args.deposit_money ,'timestamp': str(time.time())}
        if(args.withdraw_fund):
            msg = {'action':'w','account': args.account_name,'pinCode': self.randomNum ,'money':args.withdraw_fund ,'timestamp': str(time.time())}
        if(args.check_balances):
            msg = {'action':'g','account': args.account_name,'pinCode': self.randomNum ,'timestamp': str(time.time())}
        return msg

    def connect_to_bank(self, args):
        conn = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            conn.connect((args.ip, int(args.port)))
            data = str(self.getOperation(args))
            sendMessage(conn, data, self.keys)
            conn.settimeout(10.0)
            recivemessage = recieveMessage(conn, self.keys)
                  
            if recivemessage == 'False':
                sys.stderr.write('recieve msg false 255 \n')
                sys.exit(255)
            else:
                if (args.initiate):
                    self.generate_cardfile(self.cardFile)
                jsonEncoded = json.dumps(json.JSONDecoder().decode(recivemessage))
                print(jsonEncoded)
                sys.exit(0)
        except socket.error:
            sys.stderr.write('Timeout/Connection error 63 \n')
            sys.exit(63)
            
        conn.close()

    def generate_cardfile(self, cardfilename):
        with open(cardfilename, 'w') as cardFile:
            cardFile.write(self.randomNum)

    def read_cardfile(self , cardfilename):
        if os.path.isfile(cardfilename):
            with open(cardfilename, 'r') as cardFile:
                pin =  cardFile.read()
            return pin
        else:
            sys.stderr.write('cardfile file does not exists error 255 \n')
            sys.exit(255)

    def read_authfile(self, authfile):
        if os.path.isfile(authfile):
            with open(authfile, 'r') as authFile:
                self.keys = authFile.read().split('\n@@@@@')
        else:
            sys.stderr.write('cardfile file does not exists error 255 \n')
            sys.exit(255)

def main():
    parser = ArgumentParser(description='Validate')
    #Required Parameter
    parser.add_argument('-a',action='append',dest='account_name',nargs='?',help='account', required=True)   
    #Optional Parameter
    parser.add_argument('-s',action='append',dest='authfile',help='auth-file', nargs='?', const='bank.auth')
    parser.add_argument('-c',action='append',dest='cardfile',help='card-file')
    parser.add_argument('-i',action='append',dest='ip',help='ip-address', nargs='?', const='127.0.0.1')
    parser.add_argument('-p',action='append',dest='port',help='port', nargs='?', const='3000')  
    #Modes Of Operation
    g = parser.add_mutually_exclusive_group()
    g.add_argument('-n',action='append',dest='initiate',help='Balance')
    g.add_argument('-d',action='append',dest='deposit_money',help='deposit-amount')
    g.add_argument('-w',action='append',dest='withdraw_fund',help='withdraw-account')
    g.add_argument('-g',action='store_true',dest='check_balances',help='get-balance')
    
    args = parser.parse_args()

    if len(sys.argv)>len(set(sys.argv)):
        sys.stderr.write('duplication \n')
        sys.exit(255)

    #   Initial arguments validation
    if not val.preProcessRequest(args):
        sys.stderr.write('pre process error 255 \n')
        sys.exit(255)
    
    #   Validate Inputs       
    if not val.validateInput(args):
        sys.stderr.write('validate input error 255 \n')
        sys.exit(255) 

    if not (args.initiate or args.deposit_money or args.withdraw_fund or args.check_balances):
        sys.stderr.write('no operation error 255 \n')
        sys.exit(255)

    if not(args.cardfile):
        cardfile = args.account_name+'.card'
    else:
        if os.path.exists(args.cardfile):
            expectedName = args.account_name+'.card'
            if(args.cardfile == expectedName):
                cardfile = args.cardfile
            else:
                sys.stderr.write('error in card file  \n')
                sys.exit(255)
        else:
            cardfile = args.cardfile

    atm = ATM(cardfile)
    atm.read_authfile(args.authfile)

    if not (args.initiate):
        atm.randomNum = atm.read_cardfile(cardfile)
    atm.connect_to_bank(args)  

if __name__ == '__main__':
    main()

