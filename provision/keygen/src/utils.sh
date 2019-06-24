#!/bin/bash -e

workdir="/tmp/bizpoc/"
declare tmppass

# helper functions start

# $1 is participant's name
# $2 is filename
function read_from_usb {
[ "$1" ]
[ "$2" ]

echo "Please plug in $1's usb device with $2"
srcfile=$(find `mount |grep "/dev/sd[a-z]" | cut -d " " -f3` -name "$2" | head -1)
while [ ! -f "${srcfile}" ] ; do
    sleep 3;
    echo "Waiting for usb device...";
    srcfile=$(find `mount |grep "/dev/sd[a-z]" | cut -d " " -f3` -name "$2" | head -1)
done
sleep 2
srcpath=$(dirname ${srcfile})
[ -d ${srcpath} ]
[ -d ${workdir} ]
echo "Writing file from $1's usb device to $workdir";
dd if=${srcpath}/$2 of=${workdir}/$2 && sync
dd if=${srcpath}/salt of=${workdir}/salt && sync
dd if=${srcpath}/params.json of=${workdir}/params.json && sync
dd if=${srcpath}/rsaEncryptedPrivateKey of=${workdir}/rsaEncryptedPrivateKey && sync
dd if=${srcpath}/rsaPublicKey of=${workdir}/rsaPublicKey && sync
dd if=${srcpath}/xpub of=${workdir}/xpub && sync
echo "Done.";
usbdevice=$(mount |grep `dirname ${srcpath}` | cut -d " " -f1)
while [ -b /dev/"$usbdevice" ] ; do
    sleep 3;
    echo "Please take out usb device...";
done
unset usbdevice
unset srcpath
unset srcfile
}

# $1 is filename
function read_recovery_params {
[ "$1" ]

echo "Please plug in usb device with $1"
srcfile=$(find `mount |grep "/dev/sd[a-z]" | cut -d " " -f3` -name "$1" | head -1)
while [ ! -f "${srcfile}" ] ; do
    sleep 3;
    echo "Waiting for usb device...";
    srcfile=$(find `mount |grep "/dev/sd[a-z]" | cut -d " " -f3` -name "$1" | head -1)
done
sleep 2
srcpath=$(dirname ${srcfile})
[ -d ${srcpath} ]
[ -d ${workdir} ]
echo "Writing file ${srcpath}/$1 from usb device to $workdir";
[ -d ${workdir} ]
dd if=${srcpath}/"$1" of=${workdir}/"$1" && sync
echo "Done.";
usbdevice=$(mount |grep `dirname ${srcpath}` | cut -d " " -f1)
while [ -b /dev/"$usbdevice" ] ; do
    sleep 3;
    echo "Please take out usb device...";
done
unset usbdevice
unset srcpath
unset srcfile
}

# $1 is participant's name
# $2 is filename
function write_to_usb {
[ "$1" ]
[ "$2" ]

echo "Please plug in $1's usb device"
while [ ! -b /dev/"$usbdevice" ] ; do
    read -p "Enter usb device name (e.g sdb|sdb1|sdc1): " usbdevice
    sleep 3;
    echo "Waiting for usb device...";
done
sleep 2
echo "Writing ${workdir}/$2 file to $1's usb device...";
targetpath=`mount|grep "$usbdevice"|cut -d " " -f3`
[ -f ${workdir}/$2 ]
[ -d ${targetpath} ]
dd if=${workdir}/$2 of=${targetpath}/$2 && sync
dd if=${workdir}/salt of=${targetpath}/salt && sync
dd if=${workdir}/params.json of=${targetpath}/params.json && sync
dd if=${workdir}/rsaEncryptedPrivateKey of=${targetpath}/rsaEncryptedPrivateKey && sync
dd if=${workdir}/rsaPublicKey of=${targetpath}/rsaPublicKey && sync
dd if=${workdir}/xpub of=${targetpath}/xpub && sync
echo "Done.";
while [ -b /dev/"$usbdevice" ] ; do
    sleep 3;
    echo "Please take out usb device...";
done
unset usbdevice
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
