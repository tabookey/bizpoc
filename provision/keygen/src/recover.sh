#!/bin/bash -e

. `dirname $0`/utils.sh

if [ ${1} -a ${1} == "test" ]; then
echo "IN TEST MODE"
testflag="-t"
fi


mkdir -p ${workdir}

recoveryfilename="recoveryOnlineParams.json"
read_recovery_params ${recoveryfilename}

read -p "Enter Bitgo pdf full path: " bitgofile
while [ ! -f ${bitgofile} ]; do
echo "${bitgofile} doesn't exist."
read -p "Enter Bitgo pdf full path: " bitgofile
done

read -p "Enter encrypted wallet passphrase full path: " walletpassfile
while [ ! -f ${walletpassfile} ]; do
echo "${walletpassfile} doesn't exist."
read -p "Enter encrypted wallet passphrase full path: " walletpassfile
done

echo ""

encrypteduserkey=$(perl `dirname $0`/boxa.pl ${bitgofile}|head -1)
keyid=$(perl `dirname $0`/boxa.pl ${bitgofile}|tail -1)
keyfile="rsaEncryptedPrivateKey"
encryptedwalletpass=`cat ${walletpassfile}`

read -p "Enter first participant's filename: " firstfilename
read -p "Enter second participant's filename: " secondfilename

read_from_usb "first participant" "$firstfilename"
read_password "first participant"
firstpass=${tmppass}

read_from_usb "second participant" "$secondfilename"
read_password "second participant"
secondpass=${tmppass}
export firstpass secondpass

# Starting recovery
time node src/js/generate_bitcoin_keypair.js -r \
${testflag} \
-d ${workdir} \
--recovery-file ${workdir}/${recoveryfilename} \
--file1 ${workdir}/${firstfilename} --file2 ${workdir}/${secondfilename} \
            --encrypted-userkey "$encrypteduserkey" \
            --encrypted-wallet-pass "$encryptedwalletpass" \
            --key-id "$keyid" \
            --keyfile ${workdir}/${keyfile}

# Cleanup
rm -vf "${workdir}/{shareA,shareB,shareC,salt,rsaEncryptedPrivateKey,rsaPublicKey,params.json,xpub,recoveryOnlineParams.json,params.json}"
