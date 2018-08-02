#!/usr/bin/env python
#coding: utf-8

import argparse
import re

#   All Regex
portNumber = re.compile(r'^[^-,0][0-9]+$')
ip_address = re.compile(r'^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$')
valid_characters = re.compile(r'^[_\-\.0-9a-z]*$')
invalid_characters = {".", ".."}
amount = re.compile(r'^(0|[1-9][\d]*)[\.][\d]{2}$')  

#   Check for valid port
def checkPort(port):
    return bool(portNumber.match(str(port)) and 1024 < int(port) <= 65535)
   
#   Check for valid Account Name
def checkAccountName(acc_name):
    return bool( 1 <= len(acc_name) <= 122 and valid_characters.match(acc_name))

#   Check for valid File 
def validate_file(card_file):
    if card_file in invalid_characters:
        return False
    return bool(1 <= len(card_file) <= 127 and valid_characters.match(card_file))      

#   Check for valid IP address
def checkIPAddress(ip):
    return bool(ip_address.match(ip))

#   Check number
def checkNumber(number):    
    return bool(amount.match(number) and 0.00 <= float(number) <= 4294967295.99)

#   Check Initiate
def checkInitiate(number):    
    return bool(amount.match(number) and 10.00 <= float(number) <= 4294967295.99) 

#   Check Deposit Money
def checkDepositMoney(number):
    return bool(amount.match(number) and 00.00 < float(number) <= 4294967295.99)
            

#   Check Withdraw
def checkWithdraw(number):    
    return bool(amount.match(number) and 00.00 < float(number) <= 4294967295.99)      


#   Process request arguments 
def preProcessRequest(args):
    err=True
    #   Return if account_name is empty
    if(getattr(args, 'account_name')[0] is None):
        err=False
        return err
    for arg in vars(args):   
        #   Excluding bool argument -g
        if(arg is not 'check_balances'):
            attr=getattr(args, arg)
            if(arg is 'ip' and attr is None):
                setattr(args, arg, '127.0.0.1')
            if(arg is 'port' and attr is None):
                    setattr(args, arg, 3000)
            if(arg is 'authfile' and attr is None):
                setattr(args, arg, 'bank.auth')        
            if attr is not None:
                if(len(attr)>1):
                    err=False
                #   Validate argument length    
                elif(len(str(attr[0]))>4096):
                    err=False
                else:    
                #   set argument attributes    
                    setattr(args, arg, attr[0])
    return err              

# Validate all inputs
def validateInput(args):
    validator = []

    if args.initiate is not None:
        validator.append(checkInitiate(args.initiate)) 

    if args.deposit_money is not None:
        validator.append(checkDepositMoney(args.deposit_money))  
        
    if args.withdraw_fund is not None:
        validator.append(checkWithdraw(args.withdraw_fund))        

    if args.ip is not None:
        validator.append(checkIPAddress(args.ip))
        
    if args.port is not None:
        validator.append(checkPort(args.port))

    if args.account_name is not None:
        validator.append(checkAccountName(args.account_name))

    if args.cardfile is not None:
        validator.append(validate_file(args.cardfile))

    if args.authfile is not None:
        validator.append(validate_file(args.authfile))    

    if validator:
        for valid in validator:
            if not valid:
                return False
        return True 
