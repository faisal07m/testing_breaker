import re


class Balance:
    """This object can save any amount of money. It simply saves the total number of cents, to avoid
    floating point errors"""

    def __init__(self, total_cents=0):
        self.total_cents = total_cents

    def __add__(self, other):
        return Balance(self.total_cents + other.total_cents)

    def __sub__(self, other):
        return Balance(self.total_cents - other.total_cents)

    def __mul__(self, other):
        return Balance(self.total_cents * other)

    def __eq__(self, other):
        return self.total_cents == other.total_cents

    def __lt__(self, other):
        return self.total_cents < other.total_cents

    def __le__(self, other):
        return self.total_cents <= other.total_cents

    def __gt__(self, other):
        return self.total_cents > other.total_cents

    def __ge__(self, other):
        return self.total_cents >= other.total_cents

    def get_euros(self):
        """doesn't work correctly for negative balances"""
        return self.total_cents // 100

    def get_cents(self):
        """doesn't work correctly for negative balances"""
        return self.total_cents % 100

    def __str__(self):
        if self.total_cents < 0:
            return "-" + str(Balance(-self.total_cents))
        cents = self.get_cents()
        if cents == 0:
            return str(self.get_euros())
        if cents % 10 == 0:
            return str(self.get_euros()) + "." + str(cents // 10)
        str_cents = str(self.get_cents())
        if len(str_cents) < 2:
            str_cents = "0" + str_cents
        return str(self.get_euros()) + "." + str_cents

    def to_dotxx_str(self):
        if self.total_cents < 0:
            return "-" + Balance(-self.total_cents).to_dotxx_str()
        str_cents = str(self.get_cents())
        if len(str_cents) < 2:
            str_cents = "0" + str_cents
        return str(self.get_euros()) + "." + str_cents

    @staticmethod
    def from_string(s):
        """:return None on error, the correct Balance object otherwise"""
        m = re.match("(0|[1-9][0-9]*)\\.([0-9]{2})$", s)
        if not m:
            return None
        euros = int(m.group(1))
        cents = int(m.group(2))
        total_cents = euros * 100 + cents
        if total_cents > 429496729599:
            return None
        return Balance(total_cents)


if __name__ == "__main__":
    print(Balance.from_string("0"))
    print(Balance.from_string("0.00"))
    print(Balance.from_string("1.20"))
    print(Balance.from_string("2.05"))
    print(Balance.from_string("1.23") + Balance.from_string("2.22"))
    print(Balance.from_string("1.23") > Balance.from_string("0.22"))
