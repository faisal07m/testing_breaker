#!/usr/bin/env python
#coding: utf-8
import json
import argparse
import socket
import hashlib
import string
import random
import signal
import ast
import hmac
import base64
import sys
import errno
import os
from protocol import *
from Crypto.PublicKey import RSA
from Crypto.Cipher import AES
from decimal import Decimal

class Bank:
	def __init__(self,authfile , port):

		self.AES_BLOCK_SIZE = 16
		self.SIG_SIZE = hashlib.sha256().digest_size
		self.KEY_SIZE = 192
		self.aes_key = ''
		self.hmac_key = ''
		self.accounts = {}
		self.cardFiles = []
		self.port = port
		self.keys = []
		self.authFileName = authfile
		self.bankResponse = ''
		self.generateAuthFile()
		self.startServer()

		# signal.signal(signal.SIGINT, self.exitClean)
		signal.signal(signal.SIGTERM, self.exitClean)

	def generateAuthFile(self):
		if os.path.exists(self.authFileName):
			sys.stderr.write('auth file exists error 255 \n')
			sys.exit(255)

		with open (self.authFileName, 'w') as AuthFile:
			def extract_keys(key_string, key_size):
				key = key_string.decode('base64')
				return key[:-self.SIG_SIZE], key[-self.SIG_SIZE:]

			def generate_key_string(self, key_size):
				key = os.urandom(int(key_size / 8) + self.SIG_SIZE)
				return key.encode("base64").replace("\n", "")

			key_string = generate_key_string(self, self.KEY_SIZE)
			self.keys = extract_keys(key_string, self.KEY_SIZE)

			AuthFile.write(self.keys[0])
			AuthFile.write('\n@@@@@')
			AuthFile.write(self.keys[1])
		print('created')
		sys.stdout.flush()
		
	def parseMessage(self,message):
		msg = ast.literal_eval(message)
		action = msg['action']
		account = msg['account']
		cardFile = msg['pinCode']
		if ('money' in msg.keys()):
			amount = (msg['money'])
			tempAmount = Decimal(msg['money'])
			amount = round(tempAmount,2)
			if (amount).is_integer():
				amount = int(amount)
		else:
			pass

		if msg['action'] == 'n':
			self.createAccount(account,cardFile,amount)
		elif msg['action'] == 'd':
			self.depositMoney(account,cardFile,amount)
		elif msg['action'] == 'w':
			self.withdrawMoney(account,cardFile,amount)
		elif msg['action'] == 'g':
			self.getBalance(account,cardFile)
			 
	def createAccount(self, title, cardFile, amount):
		if title in self.accounts or cardFile in self.cardFiles or amount < 10.00:
			self.bankResponse = False
			return

		self.accounts[title] = {
			'cardFile': cardFile,
			'balance': amount
		}
		self.cardFiles.append(cardFile)

		toEncode = '{"account":%s,"initial_balance":%s}'%(json.dumps(title), amount)
		self.bankResponse = toEncode
		print(toEncode)

		sys.stdout.flush()

	def depositMoney(self, title, cardFile, amount):
		if not title in self.accounts or not cardFile in self.cardFiles or amount < 0.0:
			self.bankResponse = False
			return

		self.accounts[title]['balance'] = self.accounts[title]['balance'] + amount
		toEncode = '{"account":%s,"deposit":%s}'%(json.dumps(title), amount)
		self.bankResponse = toEncode
		print(toEncode)

		sys.stdout.flush()

	def withdrawMoney(self, title, cardFile, amount):
	    if not title in self.accounts or not cardFile in self.cardFiles or amount < 0.0:
	        self.bankResponse = False
	        return

	    self.accounts[title]['balance'] = self.accounts[title]['balance'] - amount
	    newAmount = round(Decimal(self.accounts[title]['balance']),2)

	    if (newAmount).is_integer():
	        self.accounts[title]['balance'] = int(newAmount)

	    if(self.accounts[title]['balance'] < 0.0):
	        self.accounts[title]['balance'] = self.accounts[title]['balance'] + amount
	        self.bankResponse = False
	        return

	    toEncode = '{"account":%s,"withdraw":%s}'%(json.dumps(title), amount)   
	    self.bankResponse = toEncode
	    print(toEncode)

	    sys.stdout.flush()

	def getBalance(self, title, cardFile):
		if not title in self.accounts or not cardFile in self.cardFiles:
			self.bankResponse = False
			return
		#{"account": "chris", "initial_balance": 4294967295.99}
		balance = self.accounts[title]['balance']

		toEncode = ('{"account":%s,"balance":%.2f}'%(json.dumps(title), balance))

		print('{"account":"'+title+'","balance":%.2f}') % balance


		self.bankResponse = toEncode
		#print('{"account":'+account+',"balance":'+balance+'}')
		sys.stdout.flush()

	def exitClean(self, signum, frame):
		self.server.shutdown(socket.SHUT_RDWR)
		self.server.close()
		sys.stderr.write('Clean Exit \n')
		sys.exit(0)

	def startServer(self):
		try:
			self.server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
			self.server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
			self.server.bind(('127.0.0.1', self.port))
			self.server.settimeout(10.0)
			self.server.listen(1)
			flag = False

			while 1:
				try:
					self.client,address = self.server.accept()
					self.client.settimeout(10.0)
					flag = True
					dencryptedMsg = recieveMessage(self.client, self.keys)
					self.parseMessage(dencryptedMsg)
					#decrypt and verify message
					sendMessage(self.client, str(self.bankResponse), self.keys)
				except socket.error:
					print('protocol_error')
					sys.stdout.flush()
				except:
					print('protocol_error')
					sys.stdout.flush()
				finally:  
					sys.stderr.write('finally client close error \n')
					# self.client.shutdown(socket.SHUT_RDWR)
					if flag:
						self.client.close()
		except:
			self.exitClean(None,None)

def main():
	parser = argparse.ArgumentParser(description='Validate')
	parser.add_argument("-s", "--auth", help = "Please provide a valid acount name", default = "bank.auth")
	parser.add_argument("-p", "--port", help = "Please provide valid port", default = 3000, type=int)
	args = parser.parse_args()

	myBank = Bank(args.auth , args.port)
	 
if __name__ == '__main__':
	main()
	
