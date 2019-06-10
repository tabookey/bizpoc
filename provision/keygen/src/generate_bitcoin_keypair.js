
bitcoinjs= require('bitcoinjs-lib');
_ref = require('triplesec'), scrypt = _ref.scrypt, pbkdf2 = _ref.pbkdf2, HMAC_SHA256 = _ref.HMAC_SHA256, WordArray = _ref.WordArray, util = _ref.util;
//console.log(bitcoinjs);
console.log("11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111");
console.log(scrypt);
console.log("22222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222");
console.log(pbkdf2);
console.log("33333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333");
console.log(HMAC_SHA256 );
console.log("44444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444");
console.log(WordArray );
console.log("55555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555");
console.log(util);
console.log("66666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666");

var HMAC_SHA256, WordArray, from_utf8, generate, iced, params, pbkdf2, run, scrypt, util, __iced_k, __iced_k_noop, _ref;
(function (Buffer){

iced = require('iced-runtime');
__iced_k = __iced_k_noop = function() {};

_ref = require('triplesec'), scrypt = _ref.scrypt, pbkdf2 = _ref.pbkdf2, HMAC_SHA256 = _ref.HMAC_SHA256, WordArray = _ref.WordArray, util = _ref.util;

//generate = require('keybase-bitcoin').generate;
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
//params.N= 20;
//params.r = 8;
//params.p = 1;
//params.c = 10000;
console.log(params)
console.log("88888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888");
spec = require('../../test/spec.json');

from_utf8 = function(s, i) {
  var b, b2, ret;
  b = new Buffer(s, 'utf8');
  b2 = Buffer.concat([b, new Buffer([i])]);
  ret = WordArray.from_buffer(b2);
  util.scrub_buffer(b);
  util.scrub_buffer(b2);
  return ret;
};

exports.run = run = function(_arg, cb) {
  var d, d2, k, obj, out, passphrase, progress_hook, s1, s2, salt, seed_final, seeds, v, ___iced_passed_deferral, __iced_deferrals, __iced_k;
  __iced_k = __iced_k_noop;
  ___iced_passed_deferral = iced.findDeferral(arguments);
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
  (function(_this) {
    return (function(__iced_k) {
      __iced_deferrals = new iced.Deferrals(__iced_k, {
        parent: ___iced_passed_deferral
      });
      scrypt(d, __iced_deferrals.defer({
        assign_fn: (function() {
          return function() {
            return s1 = arguments[0];
          };
        })(),
        lineno: 26
      }));
      __iced_deferrals._fulfill();
    });
  })(this)((function(_this) {
    return function() {
      seeds.push(s1.to_buffer());
      d2 = {
        key: from_utf8(passphrase, 2),
        salt: from_utf8(salt, 2),
        c: params.pbkdf2c,
        dkLen: params.dkLen,
        progress_hook: progress_hook,
        klass: HMAC_SHA256
      };
      (function(__iced_k) {
        __iced_deferrals = new iced.Deferrals(__iced_k, {
          parent: ___iced_passed_deferral
        });
        pbkdf2(d2, __iced_deferrals.defer({
          assign_fn: (function() {
            return function() {
              return s2 = arguments[0];
            };
          })(),
          lineno: 38
        }));
        __iced_deferrals._fulfill();
      })(function() {
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
      });
    };
  })(this));
};

}).call(this,require("buffer").Buffer)


console.log("run:",this.run);
console.log("88888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888");

const bip32 = require('bip32')
const bip39 = require('bip39')
const bs58 = require('bs58')

//const mnemonic = 'praise you muffin lion enable neck grocery crumble super myself license ghost'
const mnemonic = 'praise you muffin lion enable neck grocery crumble super myself license ghost'
const seed = bip39.mnemonicToSeed(mnemonic)
const node = bip32.fromSeed(seed)
const string = node.toBase58()
const string2 = node.neutered().toBase58()
const restored = bip32.fromBase58(string)
console.log("node:",node)
console.log("seed:",seed)
console.log("xpriv node.toBase58():", string)
console.log("xpub node.neutered().toBase58():", string2)
console.log("bip32:",bip32)

function callback(out) {
	console.log("out is:",out)
//	console.log("out.sign is:",out.sign)
//	console.log("bip32.privateKey is:",bip32.privateKey)
//	console.log("bip32.publicKey is:",bip32.publicKey)
//	console.log("bip32.sign is:",bip32.sign)
//	let node2 = bip32.fromSeed(seed)
//	let privateBytes = bs58.decode(out.private)
//	console.log("privateBytes:", privateBytes)
//	let privateHex = privateBytes.toString('hex')
//	console.log("privateHex:", privateHex)
//	let truncHex = privateHex.slice(2,privateHex.length-8)	
//	console.log("privateHex.length:", privateHex.length)
//	console.log("truncHex:", truncHex)
//	node2.__d = Buffer.from(privateHex, 'hex')
//	let truncBytes = Buffer.from(truncHex,'hex')
//	console.log("truncBytes:", truncBytes)
//	let node2 = bip32.fromSeed(truncBytes)
//	let node2 = bip32.fromSeed(privateBytes)
//	console.log("node2:", node2)
//	console.log("xpriv node2.toBase58():", node2.toBase58())
//	console.log("xpub node2..neutered().toBase58():", node2.neutered().toBase58())

	//test 
}
function callback2(out){
  console.log("callback2 out:",out);
}

args=spec.vectors[0];
console.log("args:", args);
this.run(args,callback);
function fuckthisshit(_arg){
  var d, d2, k, obj, out, passphrase, progress_hook, s1, s2, salt, seed_final, seeds, v, ___iced_passed_deferral, __iced_deferrals, __iced_k;
  __iced_k = __iced_k_noop;
  ___iced_passed_deferral = iced.findDeferral(arguments);
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
  scrypt(d,callback2);
}
//fuckthisshit(args);
