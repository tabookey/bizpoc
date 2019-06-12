const bip32 = require('bip32');
const bip39 = require('bip39');
const bs58 = require('bs58');
// const bitcoinjs= require('bitcoinjs-lib');
const BitGoJS = require('bitgo');
const bitgo = new BitGoJS.BitGo({ env: 'prod' });
const fs = require('fs');

const SALT = "PVeNVxXRRe4=";

argv = require('minimist')(process.argv, {
  alias: {
    d: 'workdir',
    p: 'restore-participant',
    r: 'recover',
    g: 'generate',
    t: 'test'
  },
  string: ["workdir", "update", "file1", "file2"]

});
_ref = require('triplesec'), scrypt = _ref.scrypt, pbkdf2 = _ref.pbkdf2, HMAC_SHA256 = _ref.HMAC_SHA256, WordArray = _ref.WordArray, util = _ref.util;

var HMAC_SHA256, WordArray, params, pbkdf2, scrypt, util, _ref;

function seedToKey(seed) {
  let key = bip32.fromSeed(seed);
  let ret = {
    "bip32key": key,
    "private"  : key.toBase58(),
    "public" : key.neutered().toBase58()
  };
  return ret;
}


params = require('../json/params.json');
// params.N= 22;
//params.r = 8;
//params.p = 1;
// params.c = 10000;
console.log("params:",params)
spec = require('../json/spec.json');

function from_utf8(s, i) {
  var b, b2, ret;
  b = new Buffer(s, 'utf8');
  b2 = Buffer.concat([b, new Buffer([i])]);
  ret = WordArray.from_buffer(b2);
  util.scrub_buffer(b);
  util.scrub_buffer(b2);
  return ret;
};
// Taken & adjusted from warpwallet's run()
async function generateBitcoinKeyPair(_arg,cb){
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

// const mnemonic = 'praise you muffin lion enable neck grocery crumble super myself license ghost';
// console.log("mnemonic :",mnemonic);
// const seed = bip39.mnemonicToSeed(mnemonic);
// const node = bip32.fromSeed(seed);
// const string = node.toBase58();
// const string2 = node.neutered().toBase58();
// const restored = bip32.fromBase58(string);
// console.log("node:",node);
// console.log("seed:",seed);
// console.log("xpriv node.toBase58():", string);
// console.log("xpub node.neutered().toBase58():", string2);
// console.log("bip32:",bip32);

function callback(out) {
	console.log("xpub is:",out.public);
	return out;
}

function saveStringToFile(filename,string) {
  fs.writeFileSync(filename, string/*, function(err) {
    if(err) {
      return console.log(err);
    }

    console.log(filename + " saved.");
  }*/);
  console.log(filename + " saved.");
}

function loadStringFromFile(filename) {
  return fs.readFileSync(filename/*, function(err,data) {
    if(err) {
      return console.log(err);
    }
    console.log(filename + " read: ",data);
    return data.toString();
  }*/).toString();

}

function getShareA(mnemonic) {
  return mnemonic.split(" ").slice(0,16).join(" ");
}

function getShareB(mnemonic) {
  return mnemonic.split(" ").slice(8,24).join(" ");
}

function getShareC(mnemonic) {
  return mnemonic.split(" ").slice(0,8).join(" ") + " " +mnemonic.split(" ").slice(16,24).join(" ");

}

async function generate(workdir="./build/" ) {
  const mnemonic = bip39.generateMnemonic(256);
  let args = {};
  args.salt = SALT;
  args.passphrase = mnemonic;
  console.log("salt:", args.salt);

  let keyPair = await generateBitcoinKeyPair(args,callback);

  /*if (argv.t) */console.log("mnemonic :",mnemonic);

  try {
    fs.mkdirSync(workdir);
  } catch (e) {
    if (e.code !== "EEXIST" )
      exit(e);
  }

  let secretShareA = getShareA(mnemonic);
  let secretShareB = getShareB(mnemonic);
  let secretShareC = getShareC(mnemonic);


  let encryptedShareA = bitgo.encrypt({ input: secretShareA, password:process.env.firstpass });
  saveStringToFile(workdir+"mnemonicA" , encryptedShareA);
  console.log("encrypted share A saved to file:", encryptedShareA);
  let decryptedShareA = bitgo.decrypt({input:loadStringFromFile(workdir+"mnemonicA"), password:process.env.firstpass});
  if (decryptedShareA !== secretShareA) {
    exit("FUCK THIS SHIT A");
  }

  let encryptedShareB = bitgo.encrypt({ input: secretShareB, password:process.env.secondpass });
  saveStringToFile(workdir+"mnemonicB" , encryptedShareB);
  console.log("encrypted share B saved to file:", encryptedShareB);
  let decryptedShareB = bitgo.decrypt({input:loadStringFromFile(workdir+"mnemonicB"), password:process.env.secondpass});
  if (decryptedShareB !== secretShareB) {
    exit("FUCK THIS SHIT B");
  }

  let encryptedShareC = bitgo.encrypt({ input: secretShareC, password:process.env.thirdpass });
  saveStringToFile(workdir+"mnemonicC" , encryptedShareC);
  console.log("encrypted share C saved to file:", encryptedShareC);
  let decryptedShareC = bitgo.decrypt({input:loadStringFromFile(workdir+"mnemonicC"), password:process.env.thirdpass});
  if (decryptedShareC !== secretShareC) {
    exit("FUCK THIS SHIT C");
  }

  saveStringToFile(workdir+"salt" , args.salt,0,args.salt.length);

  // password:process.env.firstpass = "wtf";
  // let decrypted = bitgo.decrypt({ input: encrypted, password:process.env.firstpass });
  // console.log("decrypted:",decrypted);
  // console.log("Done");

}

function restoreMnemonic(missingShare, file1, file2, firstpass, secondpass ) {

    let encryptedShare1 = loadStringFromFile(file1);
    let encryptedShare2 = loadStringFromFile(file2);
    let decrypted1 = bitgo.decrypt({ input: encryptedShare1, password: firstpass });
    console.log("decrypted1:",decrypted1);
    let decrypted2 = bitgo.decrypt({ input: encryptedShare2, password: secondpass });
    console.log("decrypted2:",decrypted2);
    let mnemonic;

    if ( missingShare == "C") {
        mnemonic = decrypted1.split(" ").slice(0,12).join(" ") + " " + decrypted2.split(" ").slice(4).join(" ");
    } else if (missingShare == "B") {
        mnemonic = decrypted1 + " " + decrypted2.split(" ").slice(8).join(" ");
    } else if (missingShare == "A") {
        mnemonic = decrypted2.split(" ").slice(0,8).join(" ") + " " + decrypted1;
    }
    console.log("Restored mnemonic:", mnemonic);
    return mnemonic;

}

async function restoreParticipant(file1, file2, workdir="./build/") {
    let fileA, fileB, fileC;

    if (file1.includes("mnemonicA")) {
        fileA = file1;
    } else
    if (file1.includes("mnemonicB")) {
        fileB = file1;
    } else if (file1.includes("mnemonicC")) {
        fileC = file1;
    }

    if (file2.includes("mnemonicA")) {
        fileA = file2;
    } else if (file2.includes("mnemonicB")) {
        fileB = file2;
    } else if (file2.includes("mnemonicC")) {
        fileC = file2;
    }
    console.log("PASSWORDS:",process.env.firstpass, process.env.secondpass);

    let mnemonic, newSecretShare, shareToRestore;
    let filePrefix = "mnemonic";
    if (!fileC) {
        shareToRestore = "C";
        mnemonic = restoreMnemonic(shareToRestore, fileA, fileB, process.env.firstpass, process.env.secondpass);
        newSecretShare = getShareC(mnemonic);

    } else if (!fileB) {
        shareToRestore = "B";
        mnemonic = restoreMnemonic(shareToRestore, fileA, fileC, process.env.firstpass, process.env.secondpass);
        newSecretShare = getShareB(mnemonic);

    } else if (!fileA) {
        shareToRestore = "A";
        mnemonic = restoreMnemonic(shareToRestore, fileB, fileC, process.env.firstpass, process.env.secondpass);
        newSecretShare = getShareA(mnemonic);

    }

    console.log("Restored share"+shareToRestore+":", newSecretShare);

    let newEncryptedShare = bitgo.encrypt({ input: newSecretShare, password: process.env.thirdpass });
    let fileToWrite = filePrefix+shareToRestore;
    saveStringToFile(workdir+fileToWrite , newEncryptedShare);
    console.log("encrypted share " + shareToRestore + " saved to file:", newEncryptedShare);

    console.log("Done");
    return fileToWrite;


}

function getBitGoBoxB(xprv, password, seed) {
  // let xprv = 'xprv9s21ZrQH143K4YNpbiHKKvA5Lhwq8dZemhynHWaiLS8gsTgq1CZem7Kyd3fHeLHiWge1cw49CYfpPEBMCN4osFBX8Ri75myVrxQaHCLpDrg';
  // let password = 'jesuschristthisisannoying';
  let derivedKey = bitgo.coin('eth').deriveKeyWithSeed({ key: xprv, seed });
  let blob = bitgo.encrypt({ input: derivedKey.key, password });
  console.log(blob);
  console.log("Done");
  return blob;
}

async function recover(file1, file2, encryptedBoxA, walletContractAddress, encryptedWalletPassphrase, destinationAddress, seed, workdir="./build/") {
  // Getting xprv from participants
  let mnemonic = restoreMnemonic(file1,file2, process.env.firstpass, process.env.secondpass);
  let args = {};
  args.salt = SALT;
  args.passphrase = mnemonic;
  console.log("salt:", args.salt);
  let keyPair = await generateBitcoinKeyPair(args,callback);
  let xprv = keyPair.private;


  // Decrypt wallet password
  let walletPassphrase = bitgo.decrypt({input: encryptedWalletPassphrase, password: xprv });

  // Decrypt boxA
  let boxA = bitgo.decrypt({ input: encryptedBoxA, password: walletPassphrase });

  // Generate encrypted blob per bitgo script
  let boxB = getBitGoBoxB(xprv, walletPassphrase, seed);


  let baseCoin = bitgo.coin('eth');
  let recoveryParams = {
    userKey: boxA,
    backupKey: boxB,
    walletContractAddress: walletContractAddress,
    walletPassphrase: walletPassphrase,
    recoveryDestination: destinationAddress
  };

  const recovery = await baseCoin.recover(recoveryParams);

  const recoveryTx = recovery.transactionHex || recovery.txHex || recovery.tx;

  if (!recoveryTx) {
    throw new Error('Fully-signed recovery transaction not detected.');
  }

  //TODO

}

async function main() {
  console.log("starting main\nargv",argv);
  if (argv.t) {
    console.log("TEST MODE - SETTING WEAK DEFAULT PASSWORDS");
    process.env.firstpass = process.env.firstpass ||'a';
    process.env.secondpass = process.env.secondpass || 'b';
    process.env.thirdpass = process.env.thirdpass || 'c';
    console.log("env vars passwd",process.env.firstpass, process.env.secondpass, process.env.thirdpass);
  }

  let workdir = argv.workdir;
  if (argv.generate) {
    console.log("Generating Bitcoin master keypair");
    await generate(workdir);
  }else if (argv.p) {
    console.log("Recovering seed of Bitcoin master keypair from 2 out of 3 participants to restoreParticipant");
    let fileRestored = await restoreParticipant(argv.file1, argv.file2, workdir);
    console.log("Restored file", fileRestored);
  }else if (argv.r) {
  console.log("Recovering seed of Bitcoin master keypair from 2 out of 3 participants to perform wallet recovery");
  await recover(argv.file1, argv.file2, argv.boxa, argv.walletcontractaddress, argv.walletpass, destAddress, seed, workdir);
}

  console.log("ending main");
}

main();
