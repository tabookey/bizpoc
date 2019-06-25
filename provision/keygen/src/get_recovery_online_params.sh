#!/bin/bash -e

. `dirname $0`/utils.sh

function display_usage {
echo -e "\nUsage:\n$0 [test] \ntest: optionally given to run on kovan testnet"
}
# check whether user had supplied -h or --help . If yes display usage \
if [[ ( $@ == "--help") ||  $@ == "-h" ]]; then
display_usage
exit 0
fi

if [ ${1} -a ${1} == "test" ]; then
echo "IN TEST MODE"
testflag="-t"
fi

read -p "Enter workdir to save generated file (default ${workdir}): " tmpworkdir
if [ -d ${tmpworkdir} ]; then
workdir=${tmpworkdir}
fi
echo "Using ${workdir}"
mkdir -p ${workdir}

read -p "Enter xpub: " xpub
read -p "Enter key id: " keyid
read -p "Enter wallet address: " walletaddress
read -p "Enter recovery destination address: " destaddress

# Starting recovery
time node src/js/get_recovery_online_params.js -r \
${testflag} \
-d ${workdir} \
--key-id "${keyid}" \
--xpub "${xpub}" \
--wallet-address "${walletaddress}" \
--dest-address "${destaddress}"


recoveryfilename="recoveryOnlineParams.json"
write_to_usb "Public recovery param" ${recoveryfilename}