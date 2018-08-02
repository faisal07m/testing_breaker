import os
import json
import subprocess
import base64
import atexit
import sys

from nbstreamreader import NonBlockingStreamReader as NBSR

def runExceution(nbsr, test_input, expectedBank, expectedAtm):
	params = test_input["input"]
	for n, j in enumerate(params):
		if "base64" in test_input and test_input["base64"]:
			params[n] = base64.standard_b64decode(params[n])
		if params[n] == "%PORT%" or params[n] == b"%PORT%":
			params[n] = "3000"
		if params[n] == "%IP%" or params[n] == b"%IP%":
			params[n] = "127.0.0.1"

	print("ATM PARAM:", str(params).encode("utf-8"))
	result = subprocess.run(["./atm"] + params, stdout=subprocess.PIPE)
	print("ATM EXIT  ", result.returncode, "STDOUT: ", result.stdout)
	try:
		atmOutJson = json.loads(result.stdout.decode('utf-8'))
	except:
		atmOutJson = result.stdout.decode('utf-8')

	bankLine = nbsr.readline(0.2) or b""
	try:
		bankOutJson = json.loads(bankLine.decode("utf-8"))
	except:
		bankOutJson = bankLine.decode("utf-8")

	atmActualJson = {"exit": result.returncode,"output": atmOutJson}
	bankActualJson = {"output": bankOutJson}

	a = compareJson("ATM", expectedAtm, atmActualJson)
	b = compareJson("BANK", expectedBank, bankActualJson)

	return a and b


def compareJson(side, expected, actual):
	if not actual == expected:
		print()
		print("ERROR", side)
		print("EXPECTED:",)
		print(expected)
		print()
		print("ACTUAL:",)
		print(actual)
		return False
	return True


def runTestcase(testplan):
	subprocess.call("rm -f *.card; rm .*.card", shell=True)
	subprocess.call("rm -f *.auth", shell=True)
	print ("Exec bank process")
	popen = subprocess.Popen(["./bank"], stdout=subprocess.PIPE)
	atexit.register(popen.kill)
	print ("Read first line")
	nbsr = NBSR(popen.stdout)
	print(nbsr.readline(5))
	error = False
	print("--"*30)

	for test in testplan:
		same = runExceution(nbsr, test["input"], test["bank"], test["atm"])
		print("--"*30)
		if not same:
			error = True
			break;
	
	subprocess.call("rm -f *.card", shell=True)
	subprocess.call("rm -f *.auth", shell=True)
	popen.terminate()
	print("BANK SIGTERM")
	popen.wait(10.0)
	if popen.returncode is None:
		popen.kill()
		print("ERROR BANK: did not exit in time")
		error = True
	elif popen.returncode != 0:
		print("ERROR BANK: did not exit with code 0")
		error = True
	else:
		print("BANK EXIT OK")
	return not(error)
	
def listTestcases():
	testDir = "testcases"
	if os.path.isdir(testDir):
		for testcase in os.listdir(testDir):
			testcasePath = os.path.join(testDir, testcase)
			if os.path.isfile(testcasePath) and testcase.endswith(".json"):
				with open(testcasePath, "rb") as f:
					testplan = json.loads(f.read().decode("utf-8"))
				if not runTestcase(testplan):
					sys.exit(255)
			
			

def extract_inputs(filename):
	with open(filename, "rb") as f:
		testplan = json.loads(f.read().decode("utf-8"))
		inputs = []
		for step in testplan:
			inputs.append(step["input"])
		print(json.dumps(inputs, ensure_ascii=True))


def merge_input_outputs(inputs_file, outputs_file):
	with open(inputs_file, "rb") as f:
		inputs = json.loads(f.read().decode("utf-8"))
	with open(outputs_file, "rb") as f:
		outputs = json.loads(f.read().decode("utf-8"))
	merged = []
	for inpt, outpt in zip(inputs, outputs):
		if "output" not in outpt["bank"]:
			outpt["bank"]["output"] = ""
		entry = {
			"input": inpt,
			"bank": outpt["bank"],
			"atm": outpt["atm"],
		}
		merged.append(entry)
	print(json.dumps(merged, ensure_ascii=True, indent=2))


if __name__ == '__main__':
	if len(sys.argv) == 3 and sys.argv[1] == "inputs":
		extract_inputs(sys.argv[2])
		sys.exit(0)
	if len(sys.argv) == 4 and sys.argv[1] == "merge":
		inputs_file = sys.argv[2]
		outputs_file = sys.argv[3]
		merge_input_outputs(inputs_file, outputs_file)
		sys.exit(0)


	listTestcases()
