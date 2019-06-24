const BitGoJS = require('bitgo');
var bitgo = new BitGoJS.BitGo({env: 'prod'});
const fs = require('fs');

const utxoLib = require('bitgo-utxo-lib');
const ethUtil = require('ethereumjs-util');

argv = require('minimist')(process.argv, {
    alias: {
        d: 'workdir',
        t: 'test'
    },
    string: ["workdir", "file1", "file2", "wallet-address", "dest-address", "xpub", "backupKeyAddress"]

});

async function main() {

    if (argv.t) {
        console.log("TEST MODE - USING KOVAN");
        bitgo = new BitGoJS.BitGo({env: 'test'});
    }

    let baseCoin = bitgo.coin('eth');
    // Set new eth tx fees (using default config values from platform)
    const gasPrice = baseCoin.getRecoveryGasPrice();
    const gasLimit = baseCoin.getRecoveryGasLimit();
    console.log("gp:", gasPrice.toString());
    console.log("gl:", gasLimit.toString());

    let key;
    if (argv["key-id"])
        key = bitgo.coin('eth').deriveKeyWithSeed({key: argv["xpub"], seed: argv["key-id"]}).key;
    else
        key = argv["xpub"];
    const backupHDNode = utxoLib.HDNode.fromBase58(key);
    let backupSigningKey = backupHDNode.getKey().getPublicKeyBuffer();
    let backupKeyAddress = argv.backupKeyAddress || `0x${ethUtil.publicToAddress(backupSigningKey, true).toString('hex')}`;
    console.log("backupKeyAddress:", backupKeyAddress);
    console.log("backupSigningKey:", backupSigningKey);

    // const backupHDNode2 = utxoLib.HDNode.fromBase58(argv["xprv"]);
    // let backupSigningKey2 = backupHDNode2.getKey().getPrivateKeyBuffer();
    // let backupKeyAddress2 = `0x${ethUtil.privateToAddress(backupSigningKey2).toString('hex')}`;
    // let backupKeyPublic2 = `${ethUtil.privateToPublic(backupSigningKey2)}`;
    // console.log("backupKeyPublic:", backupKeyPublic2);
    // console.log("backupKeyAddress:", backupKeyAddress2);

    const backupKeyNonce = await baseCoin.getAddressNonce(backupKeyAddress);
    console.log("backupKeyNonce:", backupKeyNonce.toString());

// get balance of backupKey to ensure funds are available to pay fees
    const backupKeyBalance = await baseCoin.queryAddressBalance(backupKeyAddress);
    console.log("backupKeyBalance:", backupKeyBalance.toString());
    console.log("gasPrice.mul(gasLimit)):", gasPrice.mul(gasLimit).toString());

    if (backupKeyBalance < (gasPrice.mul(gasLimit))) {
        throw new Error(`Backup key address ${backupKeyAddress} has balance ${backupKeyBalance.toString(10)}. This address must have a balance of at least 0.01 ETH to perform recoveries. Try sending some ETH to this address then retry.`);
    }

// get balance of wallet and deduct fees to get transaction amount
    const txAmount = await baseCoin.queryAddressBalance(argv["wallet-address"]);

// Get sequence ID using contract call
    const sequenceId = await baseCoin.querySequenceId(argv["wallet-address"]);

    let recoveryOnlineParams = {
        backupKeyAddress,
        backupKeyNonce,
        backupKeyBalance,
        txAmount,
        sequenceId,
        walletContractAddress: argv["wallet-address"],
        recoveryDestination: argv["dest-address"],
        xpub: argv["xpub"],


    };
    console.log("Retrieved recovery params:", recoveryOnlineParams);
    let recoveryFile = argv.workdir + "/recoveryOnlineParams.json";
    fs.writeFileSync(recoveryFile, JSON.stringify(recoveryOnlineParams));
    console.log("Recovery params written to file:", recoveryFile);
}

main();
