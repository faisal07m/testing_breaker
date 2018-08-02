import re


def is_number_valid(number_string):
    m = re.match("(0|[1-9][0-9]*)$", number_string)
    return m is not None


def is_filename_valid(filename):
    if len(filename) < 1 or len(filename) > 127:
        return False
    if filename in [".", ".."]:
        return False
    m = re.match("[_\\-.0-9a-z]*$", filename)
    return m is not None


def is_account_name_valid(account_name):
    if len(account_name) < 1 or len(account_name) > 122:
        return False
    m = re.match("[_\\-.0-9a-z]*$", account_name)
    return m is not None


def is_ip_address_valid(ip_address_string):
    m = re.match("([0-9]+)\\.([0-9]+)\\.([0-9]+)\\.([0-9]+)$", ip_address_string)
    if m is None:
        return False
    for i in range(1, 5):
        number_string = m.group(i)
        if not is_number_valid(number_string):
            return False
        number = int(number_string)
        if number > 255:
            return False
    return True


def is_port_valid(port_string):
    if not is_number_valid(port_string):
        return False
    port = int(port_string)
    return 1024 <= port <= 65535


if __name__ == "__main__":
    print(is_filename_valid(".."))
    print(is_filename_valid("afB"))
    print(is_filename_valid(""))
    print(is_filename_valid(".__ddd123.."))
    print(is_filename_valid("a" * 128))
    print(is_account_name_valid(".__ddd123.."))
    print(is_account_name_valid("a" * 123))
    print(is_port_valid("55"))
    print(is_port_valid("5555"))
    print(is_number_valid("055"))
    print(is_number_valid("66"))
    print(is_ip_address_valid("1.0.0.1"))
    print(is_ip_address_valid("01.0.0.1"))
    print(is_ip_address_valid("1.0.0.1234"))