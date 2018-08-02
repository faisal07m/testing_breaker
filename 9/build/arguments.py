import util
from balance import Balance
from myio import eprint


def preprocess_atm_arguments(args):
    """separates g arg and merges e.g. [-a, martin] to -amartin"""
    res = []
    i = 0
    while i < len(args):
        arg = args[i]
        if arg.startswith("-g"):
            res.append("-g")
            if len(arg) > 2:
                arg = "-" + arg[2:]
            else:
                i += 1
                continue
        if any(arg.startswith("-" + it) for it in "sipcadwn"):
            if len(arg) == 2:
                if i + 1 >= len(args):
                    eprint("argument is missing after", args[i])
                    return None
                if args[i + 1].startswith("-"):
                    eprint("option-argument '{}' shall not begin with -".format(args[i]))
                    return None
                res.append(arg + args[i + 1])
                i += 2
            else:
                res.append(arg)
                i += 1
        else:
            eprint("preprocessing failed at argument", args[i])
            return None
    return res


def read_atm_arguments(args):
    """should take sys.argv[1:]
    :returns (auth_filename, ip_address, port, card_filename, account_name, command [n/d/w/g], amount) on success
    :returns None on failure
    if command is g, amount will be None. if an optional argument isn't given, it will be None"""
    eprint("reading ATM arguments:", args)
    (auth_filename, ip_address, port, card_filename, account_name, command, amount) = [None] * 7
    args = preprocess_atm_arguments(args)
    if args is None:
        eprint("preprocessing or ATM arguments failed")
        return None

    def parse_command(arg):
        nonlocal command, amount
        if command is not None:
            eprint("can't define multiple commands!")
            return False
        command = arg[1]
        if command in "nwd":
            amount = Balance.from_string(arg[2:])
            if amount is None:
                eprint("amount was not parsable for", arg)
                return False
        return True

    arg_set = set()
    for arg in args:
        if arg[1] in arg_set:
            eprint("can't use same argument '{}' twice!".format(arg[1]))
            return None
        arg_set.add(arg[1])
    try:
        for arg in args:
            if arg[1] in "gnwd":
                success = parse_command(arg)
                if not success:
                    return None
            elif arg[1] == "s":
                auth_filename = arg[2:]
                if not util.is_filename_valid(auth_filename):
                    eprint("invalid auth filename", auth_filename)
                    return None
            elif arg[1] == "i":
                ip_address = arg[2:]
                if not util.is_ip_address_valid(ip_address):
                    eprint("invalid IP addresss", ip_address)
                    return None
            elif arg[1] == "p":
                port = arg[2:]
                if not util.is_port_valid(port):
                    eprint("invalid port", port)
                    return None
                port = int(port)
            elif arg[1] == "c":
                card_filename = arg[2:]
                if not util.is_filename_valid(card_filename):
                    eprint("invalid card filename", card_filename)
                    return None
            elif arg[1] == "a":
                account_name = arg[2:]
                if not util.is_account_name_valid(account_name):
                    eprint("invalid account name", account_name)
                    return None

        if command is None:
            eprint("command is missing")
            return None
        if account_name is None:
            eprint("account name is missing")
            return None
        if command in "wdn" and amount is None:
            eprint("no amount given for command", command)
            return None
        return auth_filename, ip_address, port, card_filename, account_name, command, amount
    except Exception as e:
        eprint("unexpected parsing error:", e)
        return None


def preprocess_bank_arguments(args):
    """merges e.g. [-p, 1234] to -p1234"""
    res = []
    i = 0
    while i < len(args):
        arg = args[i]
        if any(arg.startswith("-" + it) for it in "ps"):
            if len(arg) == 2:
                if i + 1 >= len(args):
                    eprint("argument is missing after", args[i])
                    return None
                if args[i + 1].startswith("-"):
                    eprint("option-argument '{}' shall not begin with -".format(args[i]))
                    return None
                res.append(arg + args[i + 1])
                i += 2
            else:
                res.append(arg)
                i += 1
        else:
            eprint("preprocessing failed at argument", args[i])
            return None
    return res


def read_bank_arguments(args):
    """should take sys.argv[1:]
    :returns (port, auth_filename) on success
    :returns None on failure
    if an optional argument isn't given, it will be None"""
    eprint("reading bank arguments:", args)
    (port, auth_filename) = [None] * 2
    args = preprocess_bank_arguments(args)
    if args is None:
        eprint("preprocessing or bank arguments failed")
        return None

    arg_set = set()
    for arg in args:
        if arg[1] in arg_set:
            eprint("can't use same argument '{}' twice!".format(arg[1]))
            return None
        arg_set.add(arg[1])
    try:
        for arg in args:
            if arg[1] == "s":
                auth_filename = arg[2:]
                if not util.is_filename_valid(auth_filename):
                    eprint("invalid auth filename", auth_filename)
                    return None
            elif arg[1] == "p":
                port = arg[2:]
                if not util.is_port_valid(port):
                    eprint("invalid port", port)
                    return None
                port = int(port)
        return port, auth_filename
    except Exception as e:
        eprint("unexpected parsing error:", e)
        return None


if __name__ == "__main__":
    print(preprocess_atm_arguments(["-garay", "-a", "martin", "-gc", "alice.card"]))
    print(preprocess_atm_arguments(["-ggaray"]))
    print(preprocess_atm_arguments(["-"]))
    print(preprocess_atm_arguments(["-g", "martin"]))
    print(preprocess_atm_arguments(["-ga"]))
    print(read_atm_arguments(["-garay", "-c", "alice.card"]))
    print(read_atm_arguments(["-n100.111", "-c", "alice.card", "-amartin"]))
    print(read_atm_arguments(["-d100.11", "-c", "alice.card", "-s", "bb.auth", "-p", "1111", "-amartin", "-p1111"]))
    print(read_atm_arguments(["-d100.11", "-c", "alice.card", "-s", "bb.auth", "-p", "1111", "-amartin"]))
    print(read_bank_arguments([]))
    print(read_bank_arguments(["-sfoo.auth"]))
    print(read_bank_arguments(["-psfoo.auth"]))
    print(read_bank_arguments(["-p1111", "-s", "foo.auth"]))
    print(preprocess_atm_arguments(["-a", "-foo"]))
