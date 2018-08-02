#!/usr/bin/python3
import base64
import json, socket
import random
import os
import signal
from os import path
import sys, getopt,re
import hashlib
import collections
from Crypto.Cipher import AES
from Crypto.Hash import SHA256
from Crypto import Random

authkey = ""

class Server(object):

	backlog = 5
	client = None
	accounts = {}
	rollbackOperation = {}

	def __init__(self):
		self.host = '127.0.0.1'
		self.port = 3000
		self.auth_file = "bank.auth"
		self.kill_now = False
		signal.signal(signal.SIGTERM, self.exit_gracefully)							#Setting the SIGTERM listener

		#Handling SIGTERM
	def exit_gracefully(self, signum, frame):
		self.kill_now = True

		#some validation of commmand line arguments and setting values for the right ones
	def getParameters(self,argv):
		if(self.has_duplicates(argv)):
			exit(255)
		for i in range(0, len(argv)):
			if argv[i] in ["-s", "-p",]:
				if(i + 1 == len(argv)):
					exit(255)
				if(str(argv[i + 1]).startswith("-")):
					exit(255)
			if str(argv[i]).startswith("-") and argv[i] not in ["-s", "-p",]:
				exit(255)
			if (str(argv[i]).startswith("-") is False) and (str(argv[i-1]).startswith("-") is False):
				exit(255)
		opts, args = getopt.getopt(argv, "s:p:")
		for opt, arg in opts:
			if opt in ["-s"]:
				self.auth_file = arg
			if opt in ["-p"]:
				self.port = arg
		return

	def validateParameters(self):
		# auth_file name validation
		if (self.auth_file == "." or self.auth_file == ".."):
			self.exitProgram()
		if (self.isWithinLength(len(self.auth_file), 1, 127) is False):
			self.exitProgram()

		if (self.isAccordingToExpr("^[_\-\.0-9a-z]+$", self.auth_file) is False):
			self.exitProgram()

		# Port validation
		if ((self.isAccordingToExpr(r"^[1-9][0-9]*$", str(self.port)) is False) or (int(self.port) < 1024) or (
				int(self.port) > 65535)):
			self.exitProgram()
		return

	def isWithinLength(self, length, lowLim, upLim):
		if(length >= lowLim and length <= upLim):
			return True
		else:
			return False

	def isAccordingToExpr(self, expression, input):
		if(re.match(expression, input) is None):
			return False
		else:
			return True

	def has_duplicates(self,list_of_values):
		value_dict = collections.defaultdict(int)
		for item in list_of_values:
			if(item in ["-s", "-p"]):
				value_dict[item] += 1
		return any(val > 1 for val in value_dict.values())

	def exitProgram(self):
		sys.exit(255)

	def setSocketProperties(self):
		self.socket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
		#self.socket.settimeout(10.0)
		self.socket.bind((self.host, int(self.port)))
		self.socket.listen(self.backlog)
		socket.setdefaulttimeout(10)

		return

	def startOperation(self):
		global authkey
		authkey = getKey(self.auth_file)

		while True:
			if self.kill_now:
				sys.exit(0)

			try:
				self.accept()
				data = self.recv()
				if data:
					response = self.decideResponse(data)
					responseToPrint = self.responseStr(response, data["command"])
					if responseToPrint != "":
						print(responseToPrint, flush=True)
					self.send(response)

			except socket.timeout:
				printResponseAndRollback()
			except socket.error:
				printResponseAndRollback()

		return

	def createAuthFile(self):
		myPath = os.getcwd()
		myPath = os.path.join(myPath,self.auth_file)
		if path.isfile(myPath) and path.isfile(myPath):
			self.exitProgram()
		passcode = os.urandom(128)[:10]
		data = hashlib.sha256(passcode).digest()
		try:
			with open(self.auth_file, 'wb') as auth:
				auth.write(data)
		except EnvironmentError as e:
			return False
		print("created", flush=True)
		return True

	def responseStr(self, res, command):
		if "error" in res:
			return ""
		filteredRes = ""
		if command == 'new_account':
			filteredRes = '{"initial_balance": '
			filteredRes += str(res["initial_balance"]) + ', '
			filteredRes += '"account": "'
			filteredRes += res["account"] + '"}'
			return filteredRes

		filteredRes = '{"account": "'

		filteredRes += res["account"] + '", '
		if command == 'deposit':
			filteredRes += '"deposit": '
			filteredRes += str(res["deposit"]) + '}'
		if command == 'get_balance':
			filteredRes += '"balance": '
			filteredRes += str(res["balance"]) + '}'
		if command == 'withdraw':
			filteredRes += '"withdraw": '
			filteredRes += str(res["withdraw"]) + '}'

		return filteredRes

	def decideResponse(self, data):
		response = {}
		if data["command"] != "new_account":
			if not self.validateRequest(data):
				response["error"] = 255
				return response

		if data["command"] == "new_account":
			response = self.newAccountRequest(data)
		if data["command"] == "get_balance":
			response = self.balanceInquiry(data)
		if data["command"] == "deposit":
			response = self.depositRequest(data)
		if data["command"] == "withdraw":
			response = self.withdrawRequest(data)

		self.rollbackOperation["account"] = data["account"]
		self.rollbackOperation["command"] = data["command"]
		self.rollbackOperation["command_args"] = data["command_args"]

		return response

	def validateRequest(self, data):
		validated = False
		if data["account"] in self.accounts:
			if self.accounts[data["account"]]["pin"] == data["pin"]:
				validated = True
		return validated

	def newAccountRequest(self, data):
		response = {}
		# check if account already created
		if data["account"] in self.accounts:
			response["error"] = 255
			return response

		new_account = {}
		new_account["pin"] = self.getRandomPin()
		new_account["current_balance"] = float(data["command_args"])
		self.accounts[data["account"]] = new_account

		response["account"] = data["account"]
		response["initial_balance"] = self.format_float(float(data["command_args"]))
		response["pin"] = new_account["pin"]

		return response

	def getRandomPin(self):
		chars = "0123456789"
		size = 4
		return ''.join(random.choice(chars) for _ in range(size))

	def depositRequest(self, data):
		response = {}
		amount = float(data["command_args"])
		if data["account"] in self.accounts and amount > 0:
			account = self.accounts[data["account"]]
			account["current_balance"] = account["current_balance"] + amount
			response["account"] = data["account"]
			response["deposit"] = self.format_float(amount)
		else:
			response["error"] = 255
		return response

	def balanceInquiry(self, data):
		response = {}
		if data["account"] in self.accounts:
			response["account"] = data["account"]
			response["balance"] = self.format_float(self.accounts[data["account"]]["current_balance"])
		else:
			response["error"] = 255
		return response

	def withdrawRequest(self, data):
		response = {}
		amount = float(data["command_args"])
		if data["account"] in self.accounts and amount > 0:
			account = self.accounts[data["account"]]
			if account["current_balance"] >= amount:
				account["current_balance"] = round(account["current_balance"] - amount,2)
				response["account"] = data["account"]
				response["withdraw"] = self.format_float(amount)
			else:
				response["error"] = 255
		else:
			response["error"] = 255
		return response

	def format_float(self, num):
		num = round(num, 2)
		return ('%i' if num == int(num) else '%s') % num

	def rollback(self):
		if not self.rollbackOperation:
			return;

		# delete the account
		if self.rollbackOperation["command"] == "new_account":
			if self.rollbackOperation["account"] in self.accounts:
				del self.accounts[self.rollbackOperation["account"]]

		# reverse the deposit
		if self.rollbackOperation["command"] == "deposit":
			if self.rollbackOperation["account"] in self.accounts:
				account = self.accounts[self.rollbackOperation["account"]]
				amount = float(self.rollbackOperation["command_args"])
				account["current_balance"] = account["current_balance"] - amount

		# reverse the withdrawal
		if self.rollbackOperation["command"] == "withdraw":
			if self.rollbackOperation["account"] in self.accounts:
				account = self.accounts[self.rollbackOperation["account"]]
				amount = float(self.rollbackOperation["command_args"])
				account["current_balance"] = account["current_balance"] + amount

		return

	def accept(self):
		# if a client is already connected, disconnect it
		if self.client:
		  self.client.close()
		self.client, self.client_addr = self.socket.accept()
		return self

	def send(self, data):
		if not self.client:
		  raise Exception('Cannot send data, no client is connected')
		_send(self.client, data, self)
		return self

	def recv(self):
		if not self.client:
		  raise Exception('Cannot receive data, no client is connected')
		return _recv(self.client, self)

	def close(self):
		if self.client:
		  self.client.close()
		  self.client = None
		if self.socket:
		  self.socket.close()
		  self.socket = None

def getKey(authFile):
	myPath = os.getcwd()
	myPath = os.path.join(myPath,authFile)
	localkey = b""
	if path.isfile(myPath) and path.isfile(myPath):
		try:
			with open(authFile, 'rb') as auth:
				localkey = auth.read()
		except EnvironmentError:
			exit(255)
	else :
		exit(255)
	return localkey

def _send(sock, data, server):
	try:
		serialized = json.dumps(data)
		encrypted_data = encrypt(authkey, str.encode(serialized))
		sock.send(('%d\n' % len(encrypted_data)).encode())
		# send the serialized data
		sock.sendall(encrypted_data.encode())
	except socket.timeout:
		printResponseAndRollback(server)
	except socket.error:
		printResponseAndRollback(server)

def _recv(sock, server):
	deserialized = {}
	try:
		# read the length of the data, letter by letter until we reach EOL
		length_str = ''
		char = sock.recv(1).decode()

		if not char:
			printResponseAndRollback(server)

		while char != '\n':
			length_str += char
			char = sock.recv(1).decode()
		total = int(length_str)

		received_data = sock.recv(total).decode()
		decrypted = decrypt(authkey, received_data)
		deserialized = json.loads(decrypted.decode())
	except socket.timeout:
		printResponseAndRollback(server)
	except socket.error:
		printResponseAndRollback(server)

	return deserialized

def printResponseAndRollback(server):
	server.rollback()
	print("protocol_error", flush=True)

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
	server = Server()
	server.getParameters(argv)
	server.validateParameters()
	server.createAuthFile()
	server.setSocketProperties()
	server.startOperation()
