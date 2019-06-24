const bip32 = require('bip32');
// const bip39 = require('bip39');
const BitGoJS = require('bitgo');
var bitgo = new BitGoJS.BitGo({env: 'prod'});
const fs = require('fs');
const path = require("path");
const randomBytes = require('crypto').randomBytes;
const crypto = require('crypto');
const ethUtil = require('ethereumjs-util');
const utxoLib = require('bitgo-utxo-lib');

const SALT = "PVeNVxXRRe4=";
const SEED_BYTE_LENGTH = 66; // should be % 3 == 0
const SEED_LENGTH = SEED_BYTE_LENGTH;

argv = require('minimist')(process.argv, {
    alias: {
        d: 'workdir',
        p: 'restore-participant',
        r: 'recover',
        g: 'generate',
        t: 'test'
    },
    string: ["workdir", "file1", "file2", "encrypted-userkey", "encrypted-wallet-pass", "key-id", "xpub", "missingshare", "keyfile"]

});

var HMAC_SHA256, WordArray, params, pbkdf2, scrypt, _util, triplesec;
triplesec = require('triplesec');
const util = require("util");
const encryptAsync = util.promisify(triplesec.encrypt);
const decryptAsync = util.promisify(triplesec.decrypt);

scrypt = triplesec.scrypt, pbkdf2 = triplesec.pbkdf2, HMAC_SHA256 = triplesec.HMAC_SHA256, WordArray = triplesec.WordArray, _util = triplesec.util;

function seedToKey(seed) {
    let key = bip32.fromSeed(seed);
    let ret = {
        "bip32key": key,
        "private": key.toBase58(),
        "public": key.neutered().toBase58()
    };
    return ret;
}


params = require('../json/params.json');
console.log("params:", params)
spec = require('../json/spec.json');

function from_utf8(s, i) {
    var b, b2, ret;
    // b = new Buffer(s, 'utf8');
    b = Buffer.from(s);
    b2 = Buffer.concat([b, new Buffer([i])]);
    ret = WordArray.from_buffer(b2);
    _util.scrub_buffer(b);
    _util.scrub_buffer(b2);
    return ret;
};

// Taken & adjusted from warpwallet's run()
async function generateBitcoinKeyPair(_arg, cb) {
    var d, d2, k, obj, out, passphrase, progress_hook, s1, s2, salt, seed_final, seeds, v;
    passphrase = _arg.passphrase, salt = _arg.salt, progress_hook = _arg.progress_hook;
    d = {};
    seeds = [];
    for (k in params) {
        v = params[k];
        d[k] = v;
    }
    d.key = from_utf8(passphrase, 1);
    d.salt = from_utf8(salt, 1);
    d.progress_hook = progress_hook;
    await new Promise(resolve => {
        scrypt(d, response => {
            s1 = response;
            resolve(response);
        });
    });
    seeds.push(s1.to_buffer());
    d2 = {
        key: from_utf8(passphrase, 2),
        salt: from_utf8(salt, 2),
        c: params.pbkdf2c,
        dkLen: params.dkLen,
        progress_hook: progress_hook,
        klass: HMAC_SHA256
    };

    await new Promise(resolve => {
        pbkdf2(d2, response => {
            s2 = response;
            resolve(response);
        });
    });

    var _i, _len, _ref1;
    seeds.push(s2.to_buffer());
    s1.xor(s2, {});
    seed_final = s1.to_buffer();
    seeds.push(seed_final);
    _ref1 = [s1, s2, d.key, d2.key];
    for (_i = 0, _len = _ref1.length; _i < _len; _i++) {
        obj = _ref1[_i];
        obj.scrub();
    }
    out = seedToKey(seed_final);
    out.seeds = seeds;
    return cb(out);

}

function callback(out) {
    console.log("xpub is:", out.public);
    return out;
}

function getShareA(seed) {
    return seed.slice(0, 2 * SEED_LENGTH / 3);
}

function getShareB(seed) {
    return seed.slice(SEED_LENGTH / 3);
}

function getShareC(seed) {
    return Buffer.concat([seed.slice(0, SEED_LENGTH / 3), seed.slice(2 * SEED_LENGTH / 3)]);
}

async function saveSecretShare(secretShare, password, filename) {
    let encryptedShare = await encryptAsync({
        key: Buffer.from(password),
        data: Buffer.from(secretShare)
    });
    fs.writeFileSync(filename, encryptedShare);
    console.log("encrypted share saved to file " + filename);
    let decryptedShare = await decryptAsync({
        key: Buffer.from(password),
        data: fs.readFileSync(filename)
    });
    if (!decryptedShare.equals(secretShare)) {
        process.exit("Error in decrypting share " + filename);
    }
}

async function generate(workdir = "./build/") {
    const seed = randomBytes(SEED_BYTE_LENGTH);
    let args = {};
    args.salt = SALT;
    args.passphrase = seed;
    console.log("salt:", args.salt);

    let keyPair = await generateBitcoinKeyPair(args, callback);

    try {
        fs.mkdirSync(workdir);
    } catch (e) {
        if (e.code !== "EEXIST")
            process.exit(e);
    }

    let secretShareA = getShareA(seed);
    let secretShareB = getShareB(seed);
    let secretShareC = getShareC(seed);

    saveSecretShare(secretShareA, process.env.firstpass, workdir + "shareA");
    saveSecretShare(secretShareB, process.env.secondpass, workdir + "shareB");
    await saveSecretShare(secretShareC, process.env.thirdpass, workdir + "shareC");

    fs.writeFileSync(workdir + "salt", args.salt);
    fs.writeFileSync(workdir + "params.json", JSON.stringify(params));
    console.log("Saved salt to file.:", args.salt);

    // generating & saving RSA private key to file - to encrypt the walletpassphrase
    let hash = crypto.createHash('sha256');
    let password = hash.update(keyPair.private).digest().toString("hex");
    let rsaKeyPair = crypto.generateKeyPairSync("rsa",
        {
            modulusLength: 4096,
            publicKeyEncoding: {
                type: "spki", format: "pem"
            },
            privateKeyEncoding: {
                type: "pkcs8",
                format: "pem",
                cipher: 'aes-256-cbc',
                passphrase: password,
                padding: crypto.constants.RSA_PKCS1_OAEP_PADDING
            }
        });

    fs.writeFileSync(workdir + "xpub", keyPair.public);
    console.log("Saved xpub to file:", keyPair.public);
    fs.writeFileSync(workdir + "rsaEncryptedPrivateKey", rsaKeyPair.privateKey);
    fs.writeFileSync(workdir + "rsaPublicKey", rsaKeyPair.publicKey);
    console.log("Saved rsa keypair to files.\nPublic key:", rsaKeyPair.publicKey);

    const backupHDNode = utxoLib.HDNode.fromBase58(keyPair.private);
    let backupSigningKey = backupHDNode.getKey().getPrivateKeyBuffer();
    let backupKeyAddress = `0x${ethUtil.privateToAddress(backupSigningKey).toString('hex')}`;
    fs.writeFileSync(workdir + "backupKeyAddress", backupKeyAddress);
    console.log("backupKeyAddress:", backupKeyAddress, "saved to file.");

}

async function restoreSeed(missingShare, file1, file2, firstpass, secondpass) {

    let encryptedShare1 = fs.readFileSync(file1);
    console.log("first encryptedShare loaded.");
    let encryptedShare2 = fs.readFileSync(file2);
    console.log("second encryptedShare loaded.");
    let decrypted1 = await decryptAsync({key: Buffer.from(firstpass), data: Buffer.from(encryptedShare1)});
    console.log("decrypted first part.");
    let decrypted2 = await decryptAsync({key: Buffer.from(secondpass), data: Buffer.from(encryptedShare2)});
    console.log("decrypted second part.");
    let seed;

    if (missingShare === "C") {
        seed = Buffer.concat([decrypted1, decrypted2.slice(decrypted2.length / 2)]);
    } else if (missingShare === "B") {
        seed = Buffer.concat([decrypted1, decrypted2.slice(decrypted2.length / 2)]);
    } else if (missingShare === "A") {
        seed = Buffer.concat([decrypted2.slice(0, decrypted2.length / 2), decrypted1]);
    }
    console.log("Restored seed.");
    return seed;

}

async function restoreParticipant(file1, file2, xpub, workdir = "./build/") {
    let fileA, fileB, fileC;

    if (file1.includes("shareA")) {
        fileA = file1;
    } else if (file1.includes("shareB")) {
        fileB = file1;
    } else if (file1.includes("shareC")) {
        fileC = file1;
    }

    if (file2.includes("shareA")) {
        fileA = file2;
    } else if (file2.includes("shareB")) {
        fileB = file2;
    } else if (file2.includes("shareC")) {
        fileC = file2;
    }

    let seed, newSecretShare, shareToRestore;
    let filePrefix = "share";
    if (!fileC) {
        shareToRestore = "C";
        seed = await restoreSeed(shareToRestore, fileA, fileB, process.env.firstpass, process.env.secondpass);
        newSecretShare = getShareC(seed);

    } else if (!fileB) {
        shareToRestore = "B";
        seed = await restoreSeed(shareToRestore, fileA, fileC, process.env.firstpass, process.env.secondpass);
        newSecretShare = getShareB(seed);

    } else if (!fileA) {
        shareToRestore = "A";
        seed = await restoreSeed(shareToRestore, fileB, fileC, process.env.firstpass, process.env.secondpass);
        newSecretShare = getShareA(seed);

    }
    console.log("Restored share" + shareToRestore + ".");

    // validating xpub
    console.log("Validating xpub:", xpub);
    let args = {};
    args.salt = SALT;
    args.passphrase = seed;
    console.log("salt:", args.salt);
    let keyPair = await generateBitcoinKeyPair(args, callback);
    if (xpub !== keyPair.public) {
        console.log("Recovered xpub:", keyPair.public, "\ngiven xpub:", xpub);
        process.exit("Recovered xpub different from given xpub");
    }
    console.log("xpub validated.");

    let newEncryptedShare = await encryptAsync({
        key: Buffer.from(process.env.thirdpass),
        data: Buffer.from(newSecretShare)
    });
    let fileToWrite = filePrefix + shareToRestore;
    fs.writeFileSync(workdir + fileToWrite, newEncryptedShare);
    console.log("encrypted share " + shareToRestore + " saved to file.");
    fs.writeFileSync(workdir + "xpub", keyPair.public);

    const backupHDNode = utxoLib.HDNode.fromBase58(keyPair.private);
    let backupSigningKey = backupHDNode.getKey().getPrivateKeyBuffer();
    let backupKeyAddress = `0x${ethUtil.privateToAddress(backupSigningKey).toString('hex')}`;
    fs.writeFileSync(workdir + "backupKeyAddress", backupKeyAddress);
    console.log("backupKeyAddress:", backupKeyAddress, "saved to file.");

    console.log("Done");
    return fileToWrite;


}

function getBitGoBoxB(xprv, password, keyID) {
    // let xprv = 'xprv9s21ZrQH143K4YNpbiHKKvA5Lhwq8dZemhynHWaiLS8gsTgq1CZem7Kyd3fHeLHiWge1cw49CYfpPEBMCN4osFBX8Ri75myVrxQaHCLpDrg';
    // let password = 'jesuschristthisisannoying';
    let derivedKey = bitgo.coin('eth').deriveKeyWithSeed({key: xprv, seed: keyID});
    let blob = bitgo.encrypt({input: derivedKey.key, password});
    console.log("blob:",blob);
    return blob;
}

function decryptStringWithRsaPrivateKey(toDecrypt, relativeOrAbsolutePathtoPrivateKey, passphrase) {
    let absolutePath = path.resolve(relativeOrAbsolutePathtoPrivateKey);
    let privateKey = {};
    privateKey.key = fs.readFileSync(absolutePath, "utf8");
    privateKey.passphrase = passphrase;//"HI";
    let buffer = Buffer.from(toDecrypt, "base64");
    let decrypted = crypto.privateDecrypt(privateKey, buffer);
    return decrypted.toString("utf8");
}

async function recover(missingShare, file1, file2, encryptedUserKey, encryptedWalletPassphrase, rsaEncryptedPrivateKeyPath, keyID, recoveryParamsFile, workdir = "./build/") {
    // Read recovery params file
    let recoveryOnlineParams = require(recoveryParamsFile);
    recoveryOnlineParams.backupKeyBalance = new ethUtil.BN(recoveryOnlineParams.backupKeyBalance,16);
    recoveryOnlineParams.txAmount = new ethUtil.BN(recoveryOnlineParams.txAmount,16);
    console.log("recoveryOnlineParams", recoveryOnlineParams);

    // Getting xprv from participants
    let seed = await restoreSeed(missingShare, file1, file2, process.env.firstpass, process.env.secondpass);
    let args = {};
    args.salt = SALT;
    args.passphrase = seed;
    console.log("salt:", args.salt);
    let keyPair = await generateBitcoinKeyPair(args, callback);
    let xprv = keyPair.private;
    // Validating xpub
    if (recoveryOnlineParams.xpub !== keyPair.public) {
        console.log("Recovered xpub:", keyPair.public, "\ngiven xpub:", recoveryOnlineParams.xpub);
        process.exit("Recovered xpub different from given xpub");
    }

    // Decrypt wallet password
    let hash = crypto.createHash('sha256');
    let password = hash.update(xprv).digest().toString("hex");
    let walletPassphrase = decryptStringWithRsaPrivateKey(encryptedWalletPassphrase, rsaEncryptedPrivateKeyPath, password);
    // let walletPassphrase = "jesuschristthisisannoying";

    // Decrypt "boxA" userkey
    let userKey = bitgo.decrypt({input: encryptedUserKey, password: walletPassphrase});

    let dummy = "dummy";
    //Re-encrypt userkey with a dummpy passphrase to create a new box A
    let reencryptedUserKey = bitgo.encrypt({input: userKey, password: dummy});
    console.log("userkey (box A):", reencryptedUserKey);

    // Generate encrypted blob per bitgo script
    let backupKey = getBitGoBoxB(xprv, dummy, keyID);
    console.log("backupKey (box B):", backupKey);


    let baseCoin = bitgo.coin('eth');
    let recoveryParams = {
        userKey: reencryptedUserKey,
        backupKey: backupKey,
        walletContractAddress: recoveryOnlineParams.walletContractAddress,
        walletPassphrase: dummy,
        recoveryDestination: recoveryOnlineParams.recoveryDestination
    };

    // Hooking bitgo functions to prevent online communication
    baseCoin.getAddressNonce = async () => {
        console.log("hooked getAddressNonce:",recoveryOnlineParams.backupKeyNonce);
        return recoveryOnlineParams.backupKeyNonce;
    };
    baseCoin.queryAddressBalance = async (address) => {
        console.log("hooked queryAddressBalance:");
        if (address === recoveryOnlineParams.walletContractAddress) {
            console.log("recoveryOnlineParams.txAmount:",recoveryOnlineParams.txAmount);
            return recoveryOnlineParams.txAmount;
        }
        if (address === recoveryOnlineParams.backupKeyAddress) {
            console.log("recoveryOnlineParams.backupKeyBalance:",recoveryOnlineParams.backupKeyBalance);
            recoveryOnlineParams.backupKeyBalance.lt = () => {
                return false;
            };
            return recoveryOnlineParams.backupKeyBalance;
        }
        recoveryOnlineParams.backupKeyBalance.lt = () => {
            return false;
        };
        return recoveryOnlineParams.backupKeyBalance;
    };
    baseCoin.querySequenceId = async () => {
        console.log("hooked querySequenceId:", recoveryOnlineParams.sequenceId);
        return recoveryOnlineParams.sequenceId;
    };

    const recovery = await baseCoin.recover(recoveryParams);
    console.log("Recovery:", recovery);

    const recoveryTx = recovery.transactionHex || recovery.txHex || recovery.tx;

    if (!recoveryTx) {
        throw new Error('Fully-signed recovery transaction not detected.');
    }

    fs.writeFileSync(workdir + "recoveryTx", recoveryTx);
    return recoveryTx;
}

async function main() {
    console.log("starting main\nargv", argv);
    if (argv.t) {
        console.log("TEST MODE - SETTING WEAK DEFAULT PASSWORDS");
        process.env.firstpass = process.env.firstpass || 'a';
        process.env.secondpass = process.env.secondpass || 'b';
        process.env.thirdpass = process.env.thirdpass || 'c';
        console.log("env vars passwd", process.env.firstpass, process.env.secondpass, process.env.thirdpass);
        console.log("TEST MODE - USING KOVAN");
        bitgo = new BitGoJS.BitGo({env: 'test'});
    }

    let workdir = argv.workdir;
    if (argv.generate) {
        console.log("Generating Bitcoin master keypair");
        await generate(workdir);
    } else if (argv.p) {
        console.log("Recovering seed of Bitcoin master keypair from 2 out of 3 participants to restoreParticipant");
        let fileRestored = await restoreParticipant(argv.file1, argv.file2, argv.xpub, workdir);
        console.log("Restored file", fileRestored);
    } else if (argv.r) {
        console.log("Recovering seed of Bitcoin master keypair from 2 out of 3 participants to perform wallet recovery");
        let missingShare;
        if (argv.file1.includes("A")) {
            if (argv.file2.includes("B")) {
                missingShare = "C";
            } else {
                missingShare = "B";
            }
        } else if (argv.file1.includes("B")) {
            if (argv.file2.includes("A")) {
                missingShare = "C";
            } else {
                missingShare = "A";
            }
        } else if (argv.file1.includes("C")) {
            if (argv.file2.includes("A")) {
                missingShare = "B";
            } else {
                missingShare = "A";
            }
        }
        let recoveryTx = await recover(missingShare, argv.file1, argv.file2, argv["encrypted-userkey"], argv["encrypted-wallet-pass"],
            argv["keyfile"], argv["key-id"], argv["recovery-file"], workdir);
        console.log("RecoveryTx:", recoveryTx);
    }
    console.log("ending main");
}

main();
