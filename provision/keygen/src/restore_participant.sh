#!/bin/bash -e

. `dirname $0`/utils.sh

read -p "Enter first participant's filename: " firstfilename
read -p "Enter second participant's filename: " secondfilename

mkdir -p ${workdir}

filestoread="salt params.json rsaEncryptedPrivateKey rsaPublicKey xpub"

read_from_usb "first participant" "$firstfilename" `echo ${filestoread}`
read_password "first participant"
firstpass=${tmppass}

read_from_usb "second participant" "$secondfilename" `echo ${filestoread}`
read_password "second participant"
secondpass=${tmppass}

read_password "Restored participant"
thirdpass=${tmppass}

read -p "Enter xpub to validate: " xpub

# Restore participant in node script, saving it to $workdir
export firstpass secondpass thirdpass
restored_file=`time node src/js/generate_bitcoin_keypair.js -p -d ${workdir} --file1 "${workdir}/$firstfilename" --file2 "${workdir}/$secondfilename" --xpub ${xpub} |tee /dev/stderr |grep "Restored file" | cut -d " " -f3`

write_to_usb "Restored participant" "${restored_file}"

echo "Finished restoring participant $3 (file ${restored_file})"