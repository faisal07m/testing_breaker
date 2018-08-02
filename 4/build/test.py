import unittest
import subprocess


class TestExitCodes(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        subprocess.call("rm -f *.card", shell=True)
        # Start bank server in background
        cls.bank = subprocess.Popen(['bash', 'bank'])
        subprocess.call(['bash', 'atm', '-a', 'foo', '-n', '10.00'])

    @classmethod
    def tearDownClass(cls):
        cls.bank.kill()

    def test_duplicate_account(self):
        second = subprocess.call(['bash', 'atm', '-c', 'aoe.card', '-a', 'foo', '-n', '10.00'])
        self.assertEqual(second, 255)

    def test_invalid_arguments(self):
        create = subprocess.call(['bash', 'atm', '-a', 'aoe', '-n', '9.00'])
        withdraw = subprocess.call(['bash', 'atm', '-a', 'foo', '-w', '-20.00'])
        deposit = subprocess.call(['bash', 'atm', '-a', 'foo', '-d', '-20.00'])
        self.assertEqual(create, 255)
        self.assertEqual(withdraw, 255)
        self.assertEqual(deposit, 255)

    def test_insufficient_balance(self):
        withdraw = subprocess.call(['bash', 'atm', '-a', 'foo', '-w', '11.00'])
        self.assertEqual(withdraw, 255)

    def test_wrong_card_file(self):
        withdraw = subprocess.call(['bash', 'atm', '-a', 'aoe', '-c', 'foo.card', '-w', '10.00'])
        self.assertEqual(withdraw, 255)


if __name__ == '__main__':
    unittest.main()
