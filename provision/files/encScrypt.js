//should be included from html, after:
//<script src="sjcl.js"></script>
//<script src="scrypt-async.js"></script>


//helper: format array of numbers into a string: each number is modulu radix, and then
// generates a digit/letter
// radix: up to 36
function arrayToString(arr,radix) {
  return Array.from(arr).map(c=>(c%radix).toString(radix)).join("")
}

//create a random string
// count - length of string
// radix - character set to use, up to 36
function randomString(count,radix) {
  arr = window.crypto.getRandomNumbers(new Uint32Array(count))
  return arrayToString(arr,radix)
}

//internal wrapper for scrypt function, used by both encrypt and decrypt methods.
// @param password - password to use
// @param options - N,r,p,salt - all required, and all passed to output
// @param cb: callback to call when done. returns { N,r,p,salt,enc}
//    that is, "enc" the encrypted block (in base64), and also all parameters needed to run scrypt for decode
function scryptWrapper(pwd, options, cb) {
  soptions = Object.assign({encoding:"base64"},options)
  console.log("Will wrap password = " + pwd)
  scrypt( pwd,
      options.salt,
      soptions,
      (enc)=> cb( { enc, scryptOptions:options } )
  )
}

//wrap password with scrypt, then encode the string with it.
// @param pwd - string password
// @param cb - callback function(error, encodedString)
//     NOTE: encodedString is: {enc, scryptOptions}, but should be treated as verbatim string.
function encryptWithScrypt( pwd, plaintext, cb) {
  salt="salt" //TODO: better salt?
  let scryptOptions = {
     salt,
     N: 8192,   // CPU/memory cost parameter, must be power of two
                // (alternatively, you can specify logN)
     r: 64,     // block size
     p: 4,      // parallelization parameter
     dkLen: 32   // length of derived key, default = 32
     // encoding:  "base64" //- standard Base64 encoding
                     // "hex" — hex encoding,
                     // "binary" — Uint8Array,
                     // undefined/null - Array of bytes
     // interruptStep: // optional, steps to split calculations (default is 0)

  }

  scryptWrapper(pwd, scryptOptions, encBlock=> {
    try {
      result = JSON.parse(sjcl.encrypt(encBlock.enc, plaintext, {iter:1000}, {}))
      cb( null, JSON.stringify({ enc : result, scryptOptions }) )
    } catch (e) {
      cb( e,null)
    }
  })
}


function calculateChecksum(password, yubikey) {
  var joined = password + yubikey;
  var hashBitsArray = sjcl.hash.sha256.hash(joined);
  var number = sjcl.bitArray.bitSlice(hashBitsArray, 0, 32)[0];
  var checksum = number % 10000;
  console.log(checksum);
  return checksum;
}

//wrap password with scrypt, and then decrypt back original string.
// @param pwd - string password
// @param encrypted - encoded string, which is the json block returned by encryptWithScript
// @param cb - function(error,plaintext)
//    return the plaintext passed to encryptWithScript (unless there was an error...
function decryptWithScrypt(pwd, encrypted, cb) {

  let encJson
  try {
    encJson = JSON.parse(encrypted)
  } catch(e) {
    cb(e,null)
  }

  scryptWrapper(pwd, encJson.scryptOptions, encBlock=> {
    try {
      console.log("password = " + encBlock.enc)
      result = sjcl.decrypt( encBlock.enc, JSON.stringify(encJson.enc))
      cb( null, result )
    } catch (e) {
      cb( e,null)
    }
  })
}
