#!/bin/bash -e
read -s -p "Enter Liraz's Password: " lirazpass
echo ""
read -s -p "Re-enter Password: " lirazpass2
echo ""
if [ "${lirazpass}" != "${lirazpass2}" ]; then
    echo "Wrong password. Exiting."
    exit 1
fi

read -s -p "Enter Yoav's Password: " yoavpass
echo ""
read -s -p "Re-enter Password: " yoavpass2 
echo ""
if [ "${yoavpass}" != "${yoavpass2}" ]; then
    echo "Wrong password. Exiting."
    exit 1
fi

read -s -p "Enter Adi's Password: " adipass
echo ""
read -s -p "Re-enter Password: " adipass2 
echo ""
if [ "${adipass}" != "${adipass2}" ]; then
    echo "Wrong password. Exiting."
    exit 1
fi
#echo $lirazpass $yoavpass $adipass
#echo $lirazpass2 $yoavpass2 $adipass2
export lirazpass yoavpass adipass

time node src/js/generate_bitcoin_keypair.js --restore-participant
