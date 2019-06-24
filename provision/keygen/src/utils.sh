#!/bin/bash -e

workdir="/tmp/bizpoc/"
declare tmppass

# helper functions start

# $1 is participant's name
# $2 is filename
function read_from_usb {
[ "$1" ]
[ "$2" ]

echo "Please plug in $1's usb device"
while [ ! -b /dev/"$usbdevice" ] ; do
    read -p "Enter usb device name (e.g sdb|sdb1|sdc1): " usbdevice
    sleep 3;
    echo "Waiting for usb device...";
done
sleep 2
echo "Writing file from $1's usb device to $workdir";
srcpath=`mount|grep "$usbdevice"|cut -d " " -f3`
[ -f ${srcpath}/$2 ]
[ -d ${workdir} ]
dd if=${srcpath}/$2 of=${workdir}/$2 && sync
dd if=${srcpath}/salt of=${workdir}/salt && sync
dd if=${srcpath}/params.json of=${workdir}/params.json && sync
dd if=${srcpath}/rsaEncryptedPrivateKey of=${workdir}/rsaEncryptedPrivateKey && sync
dd if=${srcpath}/rsaPublicKey of=${workdir}/rsaPublicKey && sync
dd if=${srcpath}/xpub of=${workdir}/xpub && sync
echo "Done.";
while [ -b /dev/"$usbdevice" ] ; do
    sleep 3;
    echo "Please take out usb device...";
done
unset usbdevice
}

function read_recovery_params {

echo "Please plug in usb device with recovery params file"
while [ ! -b /dev/"$usbdevice" ] ; do
    read -p "Enter usb device name (e.g sdb|sdb1|sdc1): " usbdevice
    sleep 3;
    echo "Waiting for usb device...";
done
sleep 2
echo "Writing file from usb device to $workdir";
srcpath=`mount|grep "$usbdevice"|cut -d " " -f3`
[ -f ${srcpath}/"$1" ]
[ -d ${workdir} ]
dd if=${srcpath}/"$1" of=${workdir}/"$1" && sync
echo "Done.";
while [ -b /dev/"$usbdevice" ] ; do
    sleep 3;
    echo "Please take out usb device...";
done
unset usbdevice
}

#function read_from_usb_new {
#[ "$1" ]
## find `mount |grep "/dev/sd[a-z]" | cut -d " " -f3` -name "shareA"
#echo "Please plug in $1's usb device"
#while [ ! -b /dev/"$usbdevice" ] ; do
#    read -p "Enter usb device name (e.g sdb|sdb1|sdc1): " usbdevice
#    sleep 3;
#    echo "Waiting for usb device...";
#done
#sleep 2
#echo "Writing file from $1's usb device to $workdir";
#srcpath=`mount|grep "$usbdevice"|cut -d " " -f3`
#[ -f ${srcpath}/$2 ]
#[ -d ${workdir} ]
#dd if=${srcpath}/$2 of=${workdir}/$2 && sync
#dd if=${srcpath}/salt of=${workdir}/salt && sync
#dd if=${srcpath}/params.json of=${workdir}/params.json && sync
#dd if=${srcpath}/rsaEncryptedPrivateKey of=${workdir}/rsaEncryptedPrivateKey && sync
#dd if=${srcpath}/rsaPublicKey of=${workdir}/rsaPublicKey && sync
#echo "Done.";
#while [ -b /dev/"$usbdevice" ] ; do
#    sleep 3;
#    echo "Please take out usb device...";
#done
#
#}

# $1 is participant's name
# $2 is filename
# $3 is block device to write to (e.g. /dev/sdb1)
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
echo "Writing $2 file to $1's usb device...";
targetpath=`mount|grep "$usbdevice"|cut -d " " -f3`
[ -f ${workdir}/$2 ]
[ -d $targetpath ]
dd if=${workdir}/$2 of=$targetpath/$2 && sync
dd if=${workdir}/salt of=$targetpath/salt && sync
dd if=${workdir}/params.json of=$targetpath/params.json && sync
dd if=${workdir}/rsaEncryptedPrivateKey of=$targetpath/rsaEncryptedPrivateKey && sync
dd if=${workdir}/rsaPublicKey of=$targetpath/rsaPublicKey && sync
dd if=${workdir}/xpub of=$targetpath/xpub && sync
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
