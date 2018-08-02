import base64
import sys

import encryption


def my_base64encode(s):
    """takes bytes, gives base64 str back"""
    return base64.b64encode(s).decode("utf-8")


def myprint(*args, **kwargs):
    print(*args, **kwargs)
    sys.stdout.flush()


def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)
    sys.stderr.flush()


def to_json(account_name, other_key, amount):
    return "{\"account\":\"" + account_name + "\",\"" + other_key + "\":" + str(amount) + "}"


if __name__ == "__main__":
    print(my_base64encode(encryption.create_random_key()))
