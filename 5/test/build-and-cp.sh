#!/bin/bash

# Cleanup previous runs
rm -rf bank/* && rm -rf atm/*

cd ../build
make
cp bank ../test/bank/bank
cp atm ../test/atm/atm
echo "Done!"