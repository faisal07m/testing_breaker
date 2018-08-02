import json
import sys
import os

def merge(testJson):
	merged = []
	for case in testJson["inputs"]:
		input_elem = case["input"]
		atm_output = case["output"]
		bank_output = {"output":""}
		if "output" not in atm_output or atm_output["output"] == {}:
			atm_output["output"] = ""
		if atm_output["exit"] == 0:
			bank_output["output"] = atm_output["output"]
		merged.append({"input":input_elem, "atm":atm_output, "bank":bank_output})
	return merged

if __name__ == "__main__":
	for param in sys.argv[1:]:
		with open(param, "rb") as f:
			testJson = json.load(f)
		merged = merge(testJson)
		newfile = "transformed_"+os.path.basename(param)
		with open(newfile, "wb") as out:
			out.write(json.dumps(merged).replace('}}, {', '}},\n{').encode("utf-8"))