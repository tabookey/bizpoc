#!/bin/bash -e

. `dirname $0`/utils.sh
# $1 is filename of first participant
# $2 is filename of second participant
# $3 is name of new restored participant (Liraz, Yoav, Adi etc.)
[ "$1" ]
[ "$2" ]
[ "$3" ]

mkdir -p ${workdir}
read_from_usb "first participant" "$1"
read_password "first participant"
firstpass=${tmppass}

read_from_usb "second participant" "$2"
read_password "second participant"
secondpass=${tmppass}

read_password "$3"
thirdpass=${tmppass}

read -p "Enter xpub to validate: " xpub

# Restore participant in node script, saving it to $workdir
export firstpass secondpass thirdpass
restored_file=`time node src/js/generate_bitcoin_keypair.js -p -d ${workdir} --file1 ${workdir}/$1 --file2 ${workdir}/$2 --xpub ${xpub} |tee /dev/stderr |grep "Restored file" | cut -d " " -f3`

write_to_usb $3 ${restored_file}

echo "Finished restoring participant $3 (file ${restored_file})"