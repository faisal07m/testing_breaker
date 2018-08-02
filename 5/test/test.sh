#!/usr/bin/expect -f
#log_user 0

set timeout 10

set port 3333
set authFile bank.auth

# Create directories:
spawn /bin/bash -c "mkdir atm"
spawn /bin/bash -c "mkdir bank"

# Start the build and copy script to setup the tests
spawn ./build-and-cp.sh
expect "Done!\r"
expect eof

# Only clear authfile and card files (used when running without make before)
spawn /bin/bash -c "rm -f bank/\"$authFile\" && rm -f atm/\"$authFile\" && rm -f atm/*.card"

# Start the bank and wait for "created"
spawn /bin/bash -c "(cd bank && ./bank -p \"$port\" -s \"$authFile\")"
set bankPID $spawn_id
expect -i "$bankPID" "created\r"

# Copy authfile from bank to atm
spawn /bin/bash -c "cp bank/\"$authFile\" atm/\"$authFile\""
wait -i $spawn_id

# Create valid accounts for b0b (12.99) and ted (10.00)
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -n 12.99 -p \"$port\" -c \"b0b.card\" -s \"$authFile\""
expect "{\"account\":\"b0b\",\"initial_balance\":12.99}\r"
expect eof
expect -i "$bankPID" "{\"account\":\"b0b\",\"initial_balance\":12.99}\r"

spawn /bin/bash -c "cd atm && ./atm -a \"ted\" -n 10.00 -p \"$port\" -c \"ted.card\" -s \"$authFile\""
expect "{\"account\":\"ted\",\"initial_balance\":10.00}\r"
expect eof
expect -i "$bankPID" "{\"account\":\"ted\",\"initial_balance\":10.00}\r"

# Deposit money for b0b
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -d 5.01 -p \"$port\" -c \"b0b.card\" -s \"$authFile\""
set atm3PID $spawn_id
expect "{\"account\":\"b0b\",\"deposit\":5.01}\r"
expect eof
expect -i "$bankPID" "{\"account\":\"b0b\",\"deposit\":5.01}\r"

# Check b0b's current balance (12.99 + 5.01 = 18.00)
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -g -p \"$port\" -c \"b0b.card\" -s \"$authFile\""
expect "{\"account\":\"b0b\",\"balance\":18.00}\r"
expect eof
expect -i "$bankPID" "{\"account\":\"b0b\",\"balance\":18.00}\r"


# Withdraw money for b0b
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -w 18.00 -p \"$port\" -c \"b0b.card\" -s \"$authFile\""
set atm3PID $spawn_id
expect "{\"account\":\"b0b\",\"withdraw\":18.00}\r"
expect eof
expect -i "$bankPID" "{\"account\":\"b0b\",\"withdraw\":18.00}\r"

# Check b0b's current balance (18.00 - 18.00 = 0.00 )
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -g -p \"$port\" -c \"b0b.card\" -s \"$authFile\""
expect "{\"account\":\"b0b\",\"balance\":0.00}\r"
expect eof
expect -i "$bankPID" "{\"account\":\"b0b\",\"balance\":0.00}\r"


# Try to check ted's current balance with b0b's cardfile => 255
spawn /bin/bash -c "cd atm && ./atm -a \"ted\" -g -p \"$port\" -c \"b0b.card\" -s \"$authFile\""
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 255 } {
    puts "Invalid exitcode when accessing account with invalid card: $exval"
}

# Try to check ted's current balance with b0b's cardfile => 255
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -w 1000.00 -p \"$port\" -c \"b0b.card\" -s \"$authFile\""
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 255 } {
   puts "Invalid exitcode when withdrawing too much: $exval"
}

# Try create a new account with the same name and different card file => 255
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -n 1000.00 -p \"$port\" -c \"b0b2.card\" -s \"$authFile\""
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 255 } {
    puts "Invalid exitcode when duplicating account: $exval"
}


# Try create a new account with different name but existing card file => 255
spawn /bin/bash -c "cd atm && ./atm -a \"bob\" -n 1000.00 -p \"$port\" -c \"b0b.card\" -s \"$authFile\""
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 255 } {
    puts "Invalid exitcode when using existing card file for account creation: $exval"
}

# Invalid input: Missing -a => 255
spawn /bin/bash -c "cd atm && ./atm -g -p \"$port\" -c \"b0b.card\" -s \"$authFile\""
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 255 } {
    puts "Invalid exitcode when using Missing -a: $exval"
}

# Invalid input: Invalid account name => 255
spawn /bin/bash -c "cd atm && ./atm -a \"bob\" -g -p \"$port\" -c \"b0b2.card\" -s \"$authFile\""
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 255 } {
    puts "Invalid exitcode when using Invalid account name: $exval"
}

# Invalid input: Missing MOO => 255
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -p \"$port\" -c \"b0b.card\" -s \"$authFile\""
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 255 } {
    puts "Invalid exitcode when using Invalid input: Missing MOO: $exval"
}

# Invalid input: Multiple MOO => 255
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -n 100.00 -g -p \"$port\" -c \"b0b.card\" -s \"$authFile\""
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 255 } {
    puts "Invalid exitcode when using Multiple MOO: $exval"
}

# Invalid input: Invalid Number format 1 (no decimal) => 255
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -n 100 -g -p \"$port\" -c \"b0b.card\" -s \"$authFile\""
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 255 } {
    puts "Invalid exitcode when using Invalid Number format 1 (no decimal): $exval"
}

# Invalid input: Invalid Number format 2 (three decimal) => 255
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -n 100.123 -g -p \"$port\" -c \"b0b.card\" -s \"$authFile\""
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 255 } {
    puts "Invalid exitcode when using Invalid Number format 2 (three decimal): $exval"
}

# Try to connect to bank with non-existing authFile => 255
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -g -p \"$port\" -c \"b0b.card\" -s \"$authFile-abc\""
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 255 } {
    puts "Invalid exitcode when using non existing authFile: $exval"
}


# Try to connect to bank with invalid authFile => 255
spawn /bin/bash -c "cd atm &&  echo \"Some random text\" > invalid.auth && ./atm -a \"b0b\" -g -p \"$port\" -c \"b0b.card\" -s invalid.auth"
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 255 } {
    puts "Invalid exitcode when using invalid auth file: $exval"
}

# Try to connect to bank on wrong port => 63
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -g -p \"2000\" -c \"b0b.card\" -s \"$authFile\""
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 63 } {
    puts "Invalid exitcode when unable to connect to bank: $exval"
}

# Try to connect to bank on wrong ip => 63
spawn /bin/bash -c "cd atm && ./atm -a \"b0b\" -g -p \"$port\" -i \"1.1.1.1\" -c \"b0b.card\" -s \"$authFile\""
set waitval [wait -i $spawn_id]
set exval [lindex $waitval 3]

if { $exval != 63 } {
    puts "Invalid exitcode when unable to connect to bank: $exval"
}


# Send bullshit to bank 
spawn /bin/bash -c "echo 'test' | nc 127.0.0.1 \"$port\""
expect -i "$bankPID" "protocol_error"