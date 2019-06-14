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
[ "$1" ]
[ "$2" ]

echo "Please plug in $1's usb device"
while [ ! -b /dev/sdb1 ] ; do
    sleep 3;
    echo "Waiting for usb device...";
done
sleep 2
echo "Writing $2 file to $1's usb device...";
targetpath=`mount|grep sdb1|cut -d " " -f3`
[ -f ${workdir}/$2 ]
[ -d $targetpath ]
dd if=${workdir}/$2 of=$targetpath/$2 && sync
dd if=${workdir}/salt of=$targetpath/salt && sync
dd if=${workdir}/params.json of=$targetpath/params.json && sync
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
