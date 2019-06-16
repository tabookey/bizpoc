#!/bin/bash -e

. `dirname $0`/utils.sh

mkdir -p ${workdir}
read -p "Enter first participant's filename: " firstfilename
read -p "Enter second participant's filename: " secondfilename
read -p "Enter xpub: " xpub
read -p "Enter encrypted user key: " encrypteduserkey
read -p "Enter key id: " keyid
read -p "Enter encrypted wallet passphrase: " encryptedwalletpass
read -p "Enter wallet contract address: " walletaddress
read -p "Enter destination address: " destaddress

echo ""

keyfile="rsaEncryptedPrivateKey"

read_from_usb "first participant" "$firstfilename"
read_password "first participant"
firstpass=${tmppass}

read_from_usb "second participant" "$secondfilename"
read_password "second participant"
secondpass=${tmppass}
export firstpass secondpass

# Starting recovery
# recover(argv.file1, argv.file2, argv["encrypted-userkey"], argv["wallet-address"], argv["encrypted-wallet-pass"], argv["dest-address"], argv["key-id"], workdir);
time node src/js/generate_bitcoin_keypair.js -r -t -d ${workdir} --file1 ${workdir}/${firstfilename} --file2 ${workdir}/${secondfilename} \
            --encrypted-userkey "$encrypteduserkey" --wallet-address "$walletaddress" --encrypted-wallet-pass "$encryptedwalletpass" \
            --dest-address "$destaddress" --key-id "$keyid" --xpub "$xpub" --keyfile ${workdir}/${keyfile}




