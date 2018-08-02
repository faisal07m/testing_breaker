#!/usr/bin/env python3
import re
import sys
import hmac
import json

def print_dict(infos):
    '''
    Prints a dictionary in JSON format and converts the cent values of the dict
    into euros.
    '''
    kv_pairs = []
    for key in infos.keys():
        if type(infos[key]) is int:
            str_int = str(infos[key])
            euros = str_int[:-2]
            cents = str_int[-2:]
            if euros == "":
                euros = "0"
            if len(cents) == 1:
                cents = "0" + cents
            kv_pairs.append("\"" + key + "\":" + euros + '.' +
                            cents)
        else:
            kv_pairs.append("\"" + key + "\":\"" + infos[key] + "\"")
    json_output = "{" + ",".join(kv_pairs) + "}"
    print(json_output, flush=True)

def validFileName(fileName):
    if len(fileName) > 127 or re.match(r"[^_\-\.\da-z]", fileName) or r'/' in fileName  or '\\' in fileName or fileName in ['.', '..'] or any(letter.isupper() for letter in fileName):
        sys.exit(255)

def validAccountName(account, exit=True):
    if len(account) <= 0 or len(account) > 122 or re.match(r"[^_\-\.\da-z]", account) or r'/' in account or '\\' in account or any(letter.isupper() for letter in account) or(' ' in account) == True:

        if exit:
            sys.exit(255)
        else:
            return False
    return True

def validPort(port):
    if port < 1024 or port > 65535:
    
        sys.exit(255)
def isSameHash(key, dict):
    hashValue = hmac.new(key)

    myHash = dict.pop('hash', None)

    tmp = bytes(json.dumps(dict, sort_keys=True), "utf-8")
    hashValue.update(tmp)
    calcHash = hashValue.digest()

    if str(myHash) == str(calcHash):
        return True
    else:
        return False

def getHashedMessage(key, mode, sender, dict = {}):

    msg = {"mode": mode, "sender": sender}
    if bool(dict):
        msg.update(dict)

    hashValue = hmac.new(key)

    tmp = bytes(json.dumps(msg, sort_keys=True), "utf-8")
    hashValue.update(tmp)

    myHash = str(hashValue.digest())

    msg.update({"hash": myHash})
    return msg
