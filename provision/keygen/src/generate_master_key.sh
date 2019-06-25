#!/bin/bash -e

. `dirname $0`/utils.sh
# Getting Liraz's password
read_password Liraz
firstpass=$tmppass

# Getting Yoav's password
read_password Yoav
secondpass=$tmppass

# Getting Kfir's password
read_password Kfir
thirdpass=$tmppass

# Generating bitcoin keypair in node script
export firstpass secondpass thirdpass
time node src/js/generate_bitcoin_keypair.js -g -d ${workdir}

# writing encrypted seed of the bitcoin keypair to participants' usb devices
filestowrite="salt params.json rsaEncryptedPrivateKey rsaPublicKey xpub"
write_to_usb Liraz shareA `echo ${filestowrite}`
rm -vf "${workdir}/shareA"
write_to_usb Yoav shareB `echo ${filestowrite}`
rm -vf "${workdir}/shareB"
write_to_usb Kfir shareC `echo ${filestowrite}`
rm -vf "${workdir}/shareC"

# Cleanup
rm -vf "${workdir}/rsaEncryptedPrivateKey"

write_to_usb Public xpub

echo "Finished generating keypair for provisioning"
