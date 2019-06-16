#!/bin/bash -e

. `dirname $0`/utils.sh
# Getting Liraz's password
read_password Liraz
firstpass=$tmppass

# Getting Yoav's password
read_password Yoav
secondpass=$tmppass

# Getting Adi's password
read_password Adi
thirdpass=$tmppass

# Generating bitcoin keypair in node script
export firstpass secondpass thirdpass
time node src/js/generate_bitcoin_keypair.js -g -d ${workdir}

# writing encrypted seed of the bitcoin keypair to participants' usb devices
write_to_usb Liraz shareA
write_to_usb Yoav shareB
write_to_usb Adi shareC

echo "Finished generating keypair for provisioning"