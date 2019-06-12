#!/bin/bash -e

workdir="/tmp/bizpoc/"
declare tmppass

# helper functions start

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
[ $1 ]
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

# Getting Liraz's password
read_password Liraz
lirazpass=$tmppass

# Getting Yoav's password
read_password Yoav
yoavpass=$tmppass

# Getting Adi's password
read_password Adi
adipass=$tmppass

echo $lirazpass $yoavpass $adipass

# Generating bitcoin keypair in node script
export lirazpass yoavpass adipass
time node src/js/generate_bitcoin_keypair.js -g -d ${workdir}

# writing encrypted seed of the bitcoin keypair to participants' usb devices
write_to_usb Liraz mnemonicA
write_to_usb Yoav mnemonicB
write_to_usb Adi mnemonicC