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
sleep 2
echo "Writing file to Liraz's usb device...";
targetfile=`mount|grep sdb1|cut -d " " -f3`
dd if=${workdir}/mnemonicA of=$targetfile/mnemonicA && sync
dd if=${workdir}/salt of=$targetfile/salt && sync
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
sleep 2
echo "Writing file to Yoav's usb device..."; 
targetfile=`mount|grep sdb1|cut -d " " -f3`
dd if=${workdir}/mnemonicB of=$targetfile/mnemonicB && sync
dd if=${workdir}/salt of=$targetfile/salt && sync
echo "Done. Please switch to Adi's usb device";
while [ -b /dev/sdb1 ] ; do
    sleep 3;
    echo "Please change usb device...";
done

while [ ! -b /dev/sdb1 ] ; do 
    sleep 3; 
    echo "Waiting for usb device..."; 
done
sleep 2
echo "Writing file to Adi's usb device..."; 
targetfile=`mount|grep sdb1|cut -d " " -f3`
dd if=${workdir}/mnemonicC of=$targetfile/mnemonicC && sync
dd if=${workdir}/salt of=$targetfile/salt && sync
echo "Done.";


