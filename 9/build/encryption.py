import base64
import os

from Crypto import Random
from Crypto.Cipher import AES


def create_random_key(number_of_bytes=32):
    return os.urandom(number_of_bytes)


BS = 16
pad = lambda s: s + (BS - len(s) % BS) * chr(BS - len(s) % BS)
unpad = lambda s: s[:-ord(s[len(s) - 1:])]


class AESCipher:
    def __init__(self, key):
        self.key = key

    def encrypt(self, raw):
        raw = pad(raw)
        iv = Random.new().read(AES.block_size)
        cipher = AES.new(self.key, AES.MODE_CBC, iv)
        return base64.b64encode(iv + cipher.encrypt(raw))

    def decrypt(self, enc):
        enc = base64.b64decode(enc)
        iv = enc[:16]
        cipher = AES.new(self.key, AES.MODE_CBC, iv)
        return unpad(cipher.decrypt(enc[16:]))


MESSAGE_HEADER = "This text should appear at the front of every sent message, after decryption. "


def encrypt_message(message, key):
    """takes a str as message, not bytes!"""
    return AESCipher(key).encrypt(MESSAGE_HEADER + message)


def decrypt_message(encrypted_message, key):
    """:return None if the message has not been correctly encrypted with then encrypt_message function.
    Otherwise the correctly decrypted message (without header) is returned"""
    decrypted_message = AESCipher(key).decrypt(encrypted_message).decode("utf-8")
    if not decrypted_message.startswith(MESSAGE_HEADER):
        return None
    return decrypted_message[len(MESSAGE_HEADER):]


if __name__ == "__main__":
    key = create_random_key()
    print(key)
    m = encrypt_message("hello12345" * 100, key)
    print(len(m), m)
    m = decrypt_message(m, key)
    print(m)
