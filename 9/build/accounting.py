from Request import Request
from balance import Balance
from myio import eprint


class Account:
    def __init__(self, pin, initial_balance=Balance(0)):
        self.pin = pin
        self.balance = initial_balance


accounts = {}


def check_account_name_exists(account_name):
    """:returns True on success, False on failure"""
    if account_name not in accounts:
        eprint("account name '{}' doesn't exists".format(account_name))
        return False
    return True


def check_pin(request, account):
    """:returns True on success, False on failure"""
    if request.card_pin != account.pin:
        eprint("wrong pin!")
        # eprint("expected '{}', but got '{}'".format(account.pin, request.card_pin))
        return False
    return True


def create_new_account(request):
    """:return True on success, False otherwise"""
    if request.account in accounts:
        eprint("account name '{}' already exists".format(request.account))
        return False
    if request.amount < Balance(10 * 100):
        eprint("initial balance {} must be at least 10.00".format(request.amount))
        return False
    accounts[request.account] = Account(request.card_pin, request.amount)
    return True


def check_balance(request):
    """:return True on success, False otherwise"""
    if not check_account_name_exists(request.account):
        return False
    account = accounts[request.account]
    if not check_pin(request, account):
        return False
    request.amount = account.balance
    return True


def deposit(request):
    """:return True on success, False otherwise"""
    if not check_account_name_exists(request.account):
        return False
    account = accounts[request.account]
    if not check_pin(request, account):
        return False
    if request.amount <= Balance(0):
        eprint("amount {} must be larger than 0.00".format(request.amount))
        return False
    account.balance += request.amount
    return True


def withdraw(request):
    """:return True on success, False otherwise"""
    if not check_account_name_exists(request.account):
        return False
    account = accounts[request.account]
    if not check_pin(request, account):
        return False
    if request.amount <= Balance(0):
        eprint("amount {} must be larger than 0.00".format(request.amount))
        return False
    if account.balance < request.amount:
        eprint("balance '{}' not sufficient to withdraw amount '{}'".format(account.balance, request.amount))
        return False
    account.balance -= request.amount
    return True


if __name__ == "__main__":
    print(create_new_account(Request("alice", "abc", "n", Balance(1230))))
    print(create_new_account(Request("alice", "abc", "n", Balance(1230))))
    print(deposit(Request("alice", "abc", "d", Balance(100))))
    print(deposit(Request("alice", "a", "d", Balance(100))))
    print(check_balance(Request("alice", "abc", "g")))
    print(withdraw(Request("alice", "abc", "w", Balance(200))))
    print(withdraw(Request("alice", "abc1", "w", Balance(200))))
    print(check_balance(Request("alice", "abc", "g")))
