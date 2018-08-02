#!/usr/bin/env python3
import argparse
import sys


class ArgumentParser(argparse.ArgumentParser):

    def parse_args(self, args=sys.argv[1:]):
        used_args = []
        for arg in args:
            if arg in used_args:
                self.error("Argument is used twice")
            if len(arg) > 0:
                if arg[0] == '-': 
                    used_args.append(arg)
            else:
                sys.exit(255)
        return super().parse_args(args)

    def error(self, message):
        sys.exit(255)
