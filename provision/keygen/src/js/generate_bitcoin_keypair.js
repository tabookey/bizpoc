const bip32 = require('bip32');
// const bip39 = require('bip39');
const BitGoJS = require('bitgo');
var bitgo = new BitGoJS.BitGo({env: 'prod'});
const fs = require('fs');
const path = require("path");
const randomBytes = require('crypto').randomBytes;
const crypto = require('crypto');

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
    string: ["workdir", "file1", "file2", "encrypted-userkey", "wallet-address", "encrypted-wallet-pass", "dest-address", "key-id", "xpub", "missingshare", "keyfile"]

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
// params.N= 22;
//params.r = 8;
//params.p = 1;
// params.c = 10000;
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
    // console.log("arguments",arguments)
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
    // console.log("d:",d);
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
    // console.log("d2:",d2);

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

function saveDataToFile(filename, data) {
    fs.writeFileSync(filename, data);
}

function loadDataFromFile(filename) {
    return fs.readFileSync(filename);

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

function encrypt(algorithm, buffer, key) {
    // Create an initialization vector
    const iv = crypto.randomBytes(16);

    // Create a new cipher using the algorithm, key, and iv
    const cipher = crypto.createCipheriv(algorithm, key, iv);

    // Create the new (encrypted) buffer
    const result = Buffer.concat([iv, cipher.update(buffer), cipher.final()]);

    return result;
}

function decrypt(algorithm, encrypted, key) {
    // Get the iv: the first 16 bytes
    const iv = encrypted.slice(0, 16);

    // Get the rest
    encrypted = encrypted.slice(16);

    // Create a decipher
    const decipher = crypto.createDecipheriv(algorithm, key, iv);

    // Actually decrypt it
    const result = Buffer.concat([decipher.update(encrypted), decipher.final()]);

    return result;
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
            exit(e);
    }

    let secretShareA = getShareA(seed);
    let secretShareB = getShareB(seed);
    let secretShareC = getShareC(seed);

    let encryptedShareA = await encryptAsync({
        key: Buffer.from(process.env.firstpass),
        data: Buffer.from(secretShareA)
    });
    saveDataToFile(workdir + "shareA", encryptedShareA);
    console.log("encrypted share A saved to file:", encryptedShareA);
    let decryptedShareA = await decryptAsync({
        key: Buffer.from(process.env.firstpass),
        data: loadDataFromFile(workdir + "shareA")
    });
    if (!decryptedShareA.equals(secretShareA)) {
        exit("Error in decrypting share A");
    }

    let encryptedShareB = await encryptAsync({
        key: Buffer.from(process.env.secondpass),
        data: Buffer.from(secretShareB)
    });
    saveDataToFile(workdir + "shareB", encryptedShareB);
    console.log("encrypted share B saved to file:", encryptedShareB);
    let decryptedShareB = await decryptAsync({
        key: Buffer.from(process.env.secondpass),
        data: loadDataFromFile(workdir + "shareB")
    });
    if (!decryptedShareB.equals(secretShareB)) {
        exit("Error in decrypting share B");
    }

    let encryptedShareC = await encryptAsync({
        key: Buffer.from(process.env.thirdpass),
        data: Buffer.from(secretShareC)
    });
    saveDataToFile(workdir + "shareC", encryptedShareC);
    console.log("encrypted share C saved to file:", encryptedShareC);
    let decryptedShareC = await decryptAsync({
        key: Buffer.from(process.env.thirdpass),
        data: loadDataFromFile(workdir + "shareC")
    });
    if (!decryptedShareC.equals(secretShareC)) {
        exit("Error in decrypting share C");
    }

    saveDataToFile(workdir + "salt", args.salt);
    saveDataToFile(workdir + "params.json", JSON.stringify(params));
    console.log("Saved salt to file.:", args.salt);

    // generating & saving RSA private key to file - to encrypt the walletpassphrase
    let hash = crypto.createHash('sha256');
    let password = hash.update(keyPair.private).digest().toString("hex");
    let rsaKeyPair = crypto.generateKeyPairSync("rsa",
        {
            modulusLength:4096,
            publicKeyEncoding:{
                type:"spki",format:"pem"
        },
        privateKeyEncoding:{
                type:"pkcs8", format:"pem", cipher: 'aes-256-cbc', passphrase: password, padding: crypto.constants.RSA_PKCS1_OAEP_PADDING
        }});

    saveDataToFile(workdir + "xpub",keyPair.public);
    console.log("Saved xpub to file:",rsaKeyPair.publicKey);
    saveDataToFile(workdir + "rsaEncryptedPrivateKey",rsaKeyPair.privateKey);
    saveDataToFile(workdir + "rsaPublicKey",rsaKeyPair.publicKey);
    console.log("Saved rsa keypair to files.\nPublic key:",rsaKeyPair.publicKey);
    console.log("Encrypted private key:",rsaKeyPair.privateKey);

}

async function restoreSeed(missingShare, file1, file2, firstpass, secondpass) {

    let encryptedShare1 = loadDataFromFile(file1);
    console.log("encryptedShare1:", encryptedShare1);
    let encryptedShare2 = loadDataFromFile(file2);
    console.log("encryptedShare2:", encryptedShare2);
    let decrypted1 = await decryptAsync({key: Buffer.from(firstpass), data: Buffer.from(encryptedShare1)});
    console.log("decrypted first part.");
    let decrypted2 = await decryptAsync({key: Buffer.from(secondpass), data: Buffer.from(encryptedShare2)});
    console.log("decrypted second part.");
    let seed;

    if (missingShare == "C") {
        seed = Buffer.concat([decrypted1, decrypted2.slice(decrypted2.length / 2)]);
    } else if (missingShare == "B") {
        seed = Buffer.concat([decrypted1, decrypted2.slice(decrypted2.length / 2)]);
    } else if (missingShare == "A") {
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
    console.log("Validating xpub:",xpub);
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
    saveDataToFile(workdir + fileToWrite, newEncryptedShare);
    console.log("encrypted share " + shareToRestore + " saved to file:", newEncryptedShare);

    console.log("Done");
    return fileToWrite;


}

function getBitGoBoxB(xprv, password, keyID) {
    // let xprv = 'xprv9s21ZrQH143K4YNpbiHKKvA5Lhwq8dZemhynHWaiLS8gsTgq1CZem7Kyd3fHeLHiWge1cw49CYfpPEBMCN4osFBX8Ri75myVrxQaHCLpDrg';
    // let password = 'jesuschristthisisannoying';
    let derivedKey = bitgo.coin('eth').deriveKeyWithSeed({key: xprv, seed: keyID});
    let blob = bitgo.encrypt({input: derivedKey.key, password});
    console.log(blob);
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

async function recover(missingShare, file1, file2, encryptedUserKey, walletContractAddress, encryptedWalletPassphrase, rsaEncryptedPrivateKeyPath, destinationAddress, keyID, xpub, workdir = "./build/") {
    // Getting xprv from participants
    let seed = await restoreSeed(missingShare, file1, file2, process.env.firstpass, process.env.secondpass);
    let args = {};
    args.salt = SALT;
    args.passphrase = seed;
    console.log("salt:", args.salt);
    let keyPair = await generateBitcoinKeyPair(args, callback);
    let xprv = keyPair.private;
    // Validating xpub
    if (xpub !== keyPair.public) {
        console.log("Recovered xpub:", keyPair.public, "\ngiven xpub:", xpub);
        process.exit("Recovered xpub different from given xpub");
    }

    // Decrypt wallet password
    // let walletPassphrase = encryptedWalletPassphrase;//bitgo.decrypt({input: encryptedWalletPassphrase, password: xprv});
    let hash = crypto.createHash('sha256');
    let password = hash.update(xprv).digest().toString("hex");
    let walletPassphrase = decryptStringWithRsaPrivateKey(encryptedWalletPassphrase, rsaEncryptedPrivateKeyPath, password);

    // Decrypt "boxA" userkey
    let userKey = bitgo.decrypt({input: encryptedUserKey, password: walletPassphrase});

    // Generate encrypted blob per bitgo script
    let backupKey = getBitGoBoxB(xprv, walletPassphrase, keyID);
    console.log("backupKey (box B):",backupKey);


    let baseCoin = bitgo.coin('eth');
    let recoveryParams = {
        userKey: userKey,
        backupKey: backupKey,
        walletContractAddress: walletContractAddress,
        walletPassphrase: walletPassphrase,
        recoveryDestination: destinationAddress
    };

    const recovery = await baseCoin.recover(recoveryParams);
    console.log("Recovery:", recovery);

    const recoveryTx = recovery.transactionHex || recovery.txHex || recovery.tx;

    if (!recoveryTx) {
        throw new Error('Fully-signed recovery transaction not detected.');
    }
    return recoveryTx;

}

async function main() {
    console.log("starting main\nargv", argv);
    if (argv.t) {
        console.log("TEST MODE - SETTING WEAK DEFAULT PASSWORDS");
        process.env.firstpass = process.env.firstpass || 'x';
        process.env.secondpass = process.env.secondpass || 'x';
        process.env.thirdpass = process.env.thirdpass || 'x';
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
        console.log(argv.file1, argv.file2, argv["encrypted-userkey"], argv["wallet-address"], argv["encrypted-wallet-pass"],
            argv["dest-address"], argv["key-id"], argv.xpub, workdir);
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
        let recoveryTx = await recover(missingShare, argv.file1, argv.file2, argv["encrypted-userkey"], argv["wallet-address"], argv["encrypted-wallet-pass"],
            argv["keyfile"],argv["dest-address"], argv["key-id"], argv.xpub, workdir);
        console.log("RecoveryTx:", recoveryTx);

    }

    console.log("ending main");
}

main();
