from balance import Balance
from myio import to_json


class Request:
    """object to store one request. command can be n w d or g. if it is g, amount should be None.
    otherwise it should be an instance of Balance"""

    def __init__(self, account, card_pin, command, amount=None):
        self.account = account
        self.card_pin = card_pin
        self.command = command
        self.amount = amount

    def __str__(self):
        amount_str = str(self.amount) if self.amount is None else self.amount.to_dotxx_str()
        return "\t".join([self.account, str(self.card_pin), self.command, amount_str])

    @staticmethod
    def from_string(s):
        parts = s.split("\t")
        if len(parts) != 4:
            return None
        amount = Balance.from_string(parts[3])
        return Request(parts[0], parts[1], parts[2], amount)

    def json_str(self):
        other_key = {'n': "initial_balance", 'w': "withdraw", 'd': "deposit", 'g': "balance"}[self.command]
        return to_json(self.account, other_key, self.amount)
