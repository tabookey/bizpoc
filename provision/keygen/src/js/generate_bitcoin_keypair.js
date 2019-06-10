const bip32 = require('bip32')
const bip39 = require('bip39')
const bs58 = require('bs58')
bitcoinjs= require('bitcoinjs-lib');
_ref = require('triplesec'), scrypt = _ref.scrypt, pbkdf2 = _ref.pbkdf2, HMAC_SHA256 = _ref.HMAC_SHA256, WordArray = _ref.WordArray, util = _ref.util;

var HMAC_SHA256, WordArray, params, pbkdf2, scrypt, util, _ref;

function generate(seed) {
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
console.log("88888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888");
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

async function run(_arg,cb){
  var d, d2, k, obj, out, passphrase, progress_hook, s1, s2, salt, seed_final, seeds, v;
  console.log("arguments",arguments)
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
  console.log("d:",d);
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
  console.log("d2:",d2);

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
  out = generate(seed_final);
  out.seeds = seeds;
  return cb(out);

}

const mnemonic = 'praise you muffin lion enable neck grocery crumble super myself license ghost';
const seed = bip39.mnemonicToSeed(mnemonic);
const node = bip32.fromSeed(seed);
const string = node.toBase58();
const string2 = node.neutered().toBase58();
const restored = bip32.fromBase58(string);
console.log("node:",node);
console.log("seed:",seed);
console.log("xpriv node.toBase58():", string);
console.log("xpub node.neutered().toBase58():", string2);
console.log("bip32:",bip32);

function callback(out) {
	console.log("out is:",out);
}
function callback2(out){
  console.log("callback2 out:",out);
}

args=spec.vectors[0];
console.log("args:", args);


run(args,callback);