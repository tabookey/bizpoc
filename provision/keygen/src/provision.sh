#!/bin/bash -e

# Getting Liraz's password
read -s -p "Enter Liraz's Password: " lirazpass
echo ""
read -s -p "Re-enter Password: " lirazpass2
echo ""
if [ "${lirazpass}" != "${lirazpass2}" ]; then
    echo "Wrong password. Exiting."
    exit 1
fi

# Getting Yoav's password
read -s -p "Enter Yoav's Password: " yoavpass
echo ""
read -s -p "Re-enter Password: " yoavpass2 
echo ""
if [ "${yoavpass}" != "${yoavpass2}" ]; then
    echo "Wrong password. Exiting."
    exit 1
fi

# Getting Adi's password
read -s -p "Enter Adi's Password: " adipass
echo ""
read -s -p "Re-enter Password: " adipass2 
echo ""
if [ "${adipass}" != "${adipass2}" ]; then
    echo "Wrong password. Exiting."
    exit 1
fi
#echo $lirazpass $yoavpass $adipass

# Generating bitcoin keypair in node script
export lirazpass yoavpass adipass
workdir="/tmp/bizpoc/"
time node src/js/generate_bitcoin_keypair.js -g -d ${workdir}

# writing encrypted seed of the bitcoin keypair to participants' usb devices
echo "Please plug in Liraz's usb device"
while [ ! -b /dev/sdb1 ] ; do 
    sleep 3; 
    echo "Waiting for usb device..."; 
done
echo "Writing file to Liraz's usb device..."; 
dd if=${workdir}/mnemonicA of=/dev/sdb1 && sync
dd if=${workdir}/salt of=/dev/sdb1 && sync
echo "Done. Please switch to Yoav's usb device";
while [ -b /dev/sdb1 ] ; do
    sleep 3; 
    echo "Please change usb device..."; 
done

echo "Please plug in Yoav's usb device"
while [ ! -b /dev/sdb1 ] ; do 
    sleep 3; 
    echo "Waiting for usb device..."; 
done
echo "Writing file to Yoav's usb device..."; 
dd if=${workdir}/mnemonicB of=/dev/sdb1 && sync
dd if=${workdir}/salt of=/dev/sdb1 && sync
echo "Done. Please switch to Adi's usb device";
while [ -b /dev/sdb1 ] ; do
    sleep 3;
    echo "Please change usb device...";
done

while [ ! -b /dev/sdb1 ] ; do 
    sleep 3; 
    echo "Waiting for usb device..."; 
done
echo "Writing file to Adi's usb device..."; 
dd if=${workdir}/mnemonicC of=/dev/sdb1 && sync
dd if=${workdir}/salt of=/dev/sdb1 && sync
echo "Done.";


