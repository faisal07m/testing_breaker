#!user/bin/env python3
import sys


def main(error, file='core1.exits'):
    print (error)
    with open(file, "r") as exitFile:
        exits = exitFile.readlines()
        # exit = exits[0]
        # print(exits)
        with open(file, "w") as f:
            pass
        with open(file, "a") as f:
            for i in exits[1:]:
                f.write(i)
        if exit == error:
            sys.exit(0)
        else:
            sys.exit(255)


if __name__ == '__main__':
    if len(sys.argv) > 2:
        main(sys.argv[1], sys.argv[2])
    else:
        main(sys.argv[1])
