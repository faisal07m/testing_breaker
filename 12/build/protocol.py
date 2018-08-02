#!/usr/bin/python
import os
import hmac
import hashlib
import socket
import sys
from Crypto.Cipher import AES


AES_BLOCK_SIZE = 16
SIG_SIZE = hashlib.sha256().digest_size
KEY_SIZE = 192

def sendMessage(conn, msg, keys):
	def encrypt(msg):
	    aes_key, hmac_key = keys
	    pad = AES_BLOCK_SIZE - len(msg) % AES_BLOCK_SIZE
	    msg = msg + pad * chr(pad)
	    iv_bytes = os.urandom(AES_BLOCK_SIZE)
	    cypher = AES.new(aes_key, AES.MODE_CBC, iv_bytes)
	    msg = iv_bytes + cypher.encrypt(msg)
	    sig = hmac.new(hmac_key, msg, hashlib.sha256).digest()
	    return msg + sig
	encryptedMsg = encrypt(msg)
	conn.send(encryptedMsg)

def recieveMessage(conn, keys):
	encryptedMsg = conn.recv(2048)

	def decrypt(encryptedMsg):
		aes_key = keys[0]
		hmac_key = keys[1]
		sig = encryptedMsg[-SIG_SIZE:]
		encryptedMsg = encryptedMsg[:-SIG_SIZE]

		if hmac.new(hmac_key, encryptedMsg, hashlib.sha256).digest() != sig:
			sys.stderr.write('error from protocol file \n')
			sys.exit(63)

		iv_bytes = encryptedMsg[:AES_BLOCK_SIZE]
		encryptedMsg = encryptedMsg[AES_BLOCK_SIZE:]
		cypher = AES.new(aes_key, AES.MODE_CBC, iv_bytes)
		decryptedMsg = cypher.decrypt(encryptedMsg)
		return decryptedMsg[:-ord(decryptedMsg[-1])]

	recieveMsg = decrypt(encryptedMsg)
	return recieveMsg