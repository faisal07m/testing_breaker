import glob
import json
import os
import subprocess
import time

from decimal import Decimal

json_files_to_run = [
    "tests/core/core1.json",
    "tests/core/core2.json",
    "tests/core/core3.json",
    "tests/core/core4.json",
    "tests/core/core5.json",
    "tests/core/core6.json",
    "tests/core/createaccount.json",
    "tests/core/deposit.json",
    "tests/core/getbalance.json",
    "tests/core/invalid1.json",
    "tests/core/timeout1.json",
    "tests/core/withdraw.json",
    "tests/performance/performance01.json",
    "tests/performance/performance02.json",
    "tests/performance/performance03.json",
    "tests/performance/performance04.json",
    "tests/performance/performance05.json",
    "tests/performance/performance06.json",
    "tests/performance/performance07.json",
    "tests/performance/performance08.json",
    "tests/performance/performance09.json",
    "tests/performance/performance10.json"
]

json_files_to_run = [
    "tests/mytests/basic_tests.json",
    "tests/mytests/cardauth_tests.json",
    "tests/mytests/ip_port_format_tests.json",
    "tests/mytests/large_numbers_tests.json",
    "tests/mytests/long_names_tests.json",
    "tests/mytests/weird_names.json"
]

json_files_to_run = [
    "tests/mytests/large_numbers_tests.json"
]


def cleanup():
    run_command(["pkill", "-f", "bank.py"])
    run_command(["fuser", "-KILL", "-k", "-n", "tcp", "3000"])
    run_command(["fuser", "-k", "3000/tcp"])
    # all not working...
    # time.sleep(2.5)
    files = glob.glob("*.card")
    files += glob.glob("*.auth")
    files += glob.glob(".*card")
    files += glob.glob("aaaaaaaaaaaaaaaaaaaaaaaaa*")
    files += ["-c", "..."]
    for file in files:
        if os.path.isfile(file):
            os.remove(file)


port = 3000
failed_input_args = []

def run_json_file(json_file):
    global port, failed_input_args
    print(json_file)
    contents = open(json_file).read()
    port += 1
    if "%ACTUALPORT%" in contents:
        contents = contents.replace("%PORT%", str(port + 1))
        contents = contents.replace("%ACTUALPORT%", str(port))
        contents = contents.replace("%IP%", "127.0.0.2")
        contents = contents.replace("%ACTUALIP%", "127.0.0.1")
    else:
        contents = contents.replace("%PORT%", str(port))
        contents = contents.replace("%IP%", "127.0.0.1")
    j = json.loads(contents)
    cleanup()
    subprocess.Popen(["./bank", "-p", str(port)])
    time.sleep(0.5)
    successes = 0
    for entry in j["inputs"]:
        try:
            input_args = entry["input"]["input"]
            failed_input_args.append(input_args)
            (output, exit_code) = run_command(["./atm"] + input_args)
            expected_exit_code = int(entry["output"]["exit"])
            expected_output = entry["output"]["output"]
            if len(expected_output) > 0:
                expected_account = expected_output["account"]
                expected_other_key = list(it for it in expected_output.keys() if it != "account")[0]
                expected_amount = Decimal(expected_output[expected_other_key])
                output = output[:-1]
                output_json = json.loads(output)
                account = output_json["account"]
                if account != expected_account:
                    print("__FAILURE account: {} != {}, in json: {}".format(expected_account, account, output))
                    continue
                other_key = list(it for it in expected_output.keys() if it != "account")[0]
                if other_key != expected_other_key:
                    print("__FAILURE other key: {} != {}, in json: {}".format(expected_other_key, other_key, output))
                    continue
                amount = Decimal(output_json[other_key])
                if amount != expected_amount:
                    print("__FAILURE amount: {} != {}, in json: {}".format(expected_amount, amount, output))
                    continue
            elif len(output) > 0:
                print("__FAILURE output {} should have been empty".format(output))
                continue
            print(input_args)
            if exit_code != expected_exit_code:
                print("__FAILURE exit code: {} != {}".format(expected_exit_code, exit_code))
                continue
            successes += 1
            failed_input_args = failed_input_args[:-1]
            print("__SUCCESS: {} == {}, {} == {}".format(expected_exit_code, exit_code, expected_output_string,
                                                         output[:-1]))
        except Exception as e:
            print("__FAILURE exception thrown:", e)
    cleanup()
    print("success: {}/{}".format(successes, len(j["inputs"])))


def main():
    for json_file in json_files_to_run:
        run_json_file(json_file)
    print("failures:\n" + "=" * 33)
    for failed_arg in failed_input_args:
        print(failed_arg)


def run_command(command_args):
    """:returns (output, exit_code)
    call with e.g. ["echo", "foo"]"""
    try:
        res_output = subprocess.check_output(command_args)
        return res_output.decode("utf-8"), 0
    except subprocess.CalledProcessError as e:
        return e.output.decode("utf-8"), e.returncode


if __name__ == "__main__":
    main()
    pass
