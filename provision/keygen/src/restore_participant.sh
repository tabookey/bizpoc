#!/bin/bash -e

workdir="/tmp/bizpoc/"
declare tmppass

# helper functions start

# $1 is participant's name
# $2 is filename
function read_from_usb {
[ "$1" ]
[ $2 ]
echo "Please plug in $1's usb device"
while [ ! -b /dev/sdb1 ] ; do
    sleep 3;
    echo "Waiting for usb device...";
done
sleep 2
echo "Writing file from $1's usb device to $workdir";
srcpath=`mount|grep sdb1|cut -d " " -f3`
[ -f $srcpath/$2 ]
[ -d ${workdir} ]
dd if=$srcpath/$2 of=${workdir}/$2 && sync
dd if=${workdir}/salt of=$srcpath/salt && sync
echo "Done.";
while [ -b /dev/sdb1 ] ; do
    sleep 3;
    echo "Please take out usb device...";
done

}

# $1 is participant's name
# $2 is filename
function write_to_usb {
[ $1 ]
[ $2 ]

echo "Please plug in $1's usb device"
while [ ! -b /dev/sdb1 ] ; do
    sleep 3;
    echo "Waiting for usb device...";
done
sleep 2
echo "Writing file to $1's usb device...";
targetfile=`mount|grep sdb1|cut -d " " -f3`
[ -f ${workdir}/$2 ]
[ -d $targetfile ]
dd if=${workdir}/$2 of=$targetfile/$2 && sync
dd if=${workdir}/salt of=$targetfile/salt && sync
echo "Done.";
while [ -b /dev/sdb1 ] ; do
    sleep 3;
    echo "Please take out usb device...";
done
}

# $1 is participant's name
function read_password {
[ "$1" ]
read -s -p "Enter $1's Password: " tmppass
echo ""
read -s -p "Re-enter Password: " tmppass2
echo ""
if [ "${tmppass}" != "${tmppass2}" ]; then
    echo "Wrong password. Exiting."
    exit 1
fi

}

# helper functions end

# $1 is filename of first participant
# $2 is filename of second participant
# $3 is name of new restored participant (Liraz, Yoav, Adi etc.)
[ $1 ]
[ $2 ]
[ $3 ]

mkdir -p ${workdir}
read_from_usb "first participant" $1
read_password "first participant"
firstpass=${tmppass}

read_from_usb "second participant" $2
read_password "second participant"
secondpass=${tmppass}

read_password $3
newpass=${tmppass}

# Restore participant in node script, saving it to $workdir
restored_file=`time node src/js/generate_bitcoin_keypair.js -p -d ${workdir} --file1 ${workdir}/$1 --file2 ${workdir}/$2 |grep "Restored file" | cut -d " " -f3`

write_to_usb $3 ${restored_file}