
from Cryptodome.Cipher import AES
from Cryptodome.Util.Padding import pad, unpad
from Cryptodome.Random import get_random_bytes
from Cryptodome.Hash import SHA3_256
from base64 import b64encode, b64decode

IV_RANDOM_BYTE_COUNT = 8


class CipherHelper:

    def __init__(self, key_file):
        '''
        Creates a new instance of a CipherHelper using the key written in The
        file specified in key_file.
        Uses a AES cipher and uses the file's SHA3 hash to generate a 32 bytes
        key
        '''
        with open(key_file, 'rb') as f:
            self.key = SHA3_256.new().update(f.read()).digest()

    def generate_iv(self, random_bytes):
        '''
        Generates the IV from the key and a byte string

        Returns 16 bytes representing the IV
        '''
        hash = SHA3_256.new().update(random_bytes+self.key).digest()
        # Only return as many bytes as the AES block size
        return hash[0:AES.block_size]

    def encrypt(self, message):
        '''
        Takes a message as a string and returns the encrypted message as a
        string.
        Uses AES in CBC mode together with PKCS7 padding.
        The IV is generated using a number random bytes specified in
        IV_RANDOM_BYTE_COUNT and the key.

        Returns encrypted message
        '''
        random_bytes = get_random_bytes(IV_RANDOM_BYTE_COUNT)
        iv = self.generate_iv(random_bytes)
        cipher = AES.new(self.key, AES.MODE_CBC, iv)
        padded_msg = pad(message.encode('utf-8'), AES.block_size)
        enc_msg = cipher.encrypt(padded_msg)
        return b64encode(enc_msg + random_bytes).decode('utf-8')

    def decrypt(self, message):
        '''
        Takes an encrypted message and returns the original message as a
        string.

        Returns the original message
        '''
        encrypted_msg = b64decode(message)
        # last bytes are the bytes used to generate the IV
        random_bytes = encrypted_msg[-IV_RANDOM_BYTE_COUNT:]
        encrypted_msg = encrypted_msg[:-IV_RANDOM_BYTE_COUNT]

        iv = self.generate_iv(random_bytes)
        cipher = AES.new(self.key, AES.MODE_CBC, iv)
        dec_msg = cipher.decrypt(encrypted_msg)
        unpadded_msg = unpad(dec_msg, AES.block_size)
        return unpadded_msg.decode('utf-8')
