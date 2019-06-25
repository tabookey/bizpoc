#!/bin/bash -e

workdir="/tmp/bizpoc/"
declare tmppass

# helper functions start

# $1 is participant's name
# $2+ are filenames to be read from usb device
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
shift
while (( "$#" )); do
dd if=${srcpath}/"$1" of=${workdir}/"$1"  && sync && echo "${srcpath}/$1  read from usb device.";
shift
done
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
# $2+ are filenames to be written
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
echo "Writing files to $1's usb device...";
targetpath=`mount|grep "$usbdevice"|cut -d " " -f3`
[ -f ${workdir}/$2 ]
[ -d ${targetpath} ]
shift
while (( "$#" )); do
dd if=${workdir}/$1 of=${targetpath}/$1 && sync && echo "${workdir}/$1  written to usb device.";
shift
done
while [ -b /dev/"$usbdevice" ] ; do
    sleep 3;
    echo "Please take out usb device...";
done
unset usbdevice
unset targetpath
}

# $1 is participant's name
function read_password {
[ "$1" ]
read -s -p "Enter $1's Password: " tmppass
echo ""
read -s -p "Re-enter Password: " tmppass2
echo ""
while [ "${tmppass}" != "${tmppass2}" ]; do
    echo "Wrong password. Try again."
    read -s -p "Enter $1's Password: " tmppass
    echo ""
    read -s -p "Re-enter Password: " tmppass2
    echo ""
done

}

# helper functions end
