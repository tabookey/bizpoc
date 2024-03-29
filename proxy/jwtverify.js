
const axios = require('axios')
const sjcl = require('sjcl')
const fs = require('fs')
const { verySecretString, allowedSigs } = require('./proxyConfig.js')


if ( !verySecretString ) {
    console.log( "FATAL: server has no secret")
    process.exit(1)
}

if ( ! allowedSigs || !allowedSigs.length ) {
    console.log( "FATAL: server has no allowed sigs")
    process.exit(1)
}

verify_url = "https://www.googleapis.com/androidcheck/v1/attestations/verify?key="
api_key = "AIzaSyCz-RqbUmqaKhWqU12-38mTwXMNsV3rlfE"

allowedPackageNames=[
    'com.tabookey.bizpoc',
]

function parseJwt(orig) {
    arr = orig.split('.')
    part0 = JSON.parse(Buffer.from(arr[0],'base64').toString())
    if ( part0.alg != 'RS256' )
        throw Error( "JWT: Invalid alg "+part0.alg+"!= RS256")
    part1 = JSON.parse(Buffer.from(arr[1],'base64').toString())
	part1.elapsed=(new Date().getTime()-part1.timestampMs)/1000
    return part1
}

async function validateJwt(orig, requireFreshAttestation, hmac) {
    data = parseJwt(orig)
    if ( data.apkPackageName )
    if ( allowedPackageNames.indexOf(data.apkPackageName) == -1 ) {
        return {error: "invalid pkg: "+data.apkPackageName, data}
    }
    if ( allowedSigs.indexOf(data.apkCertificateDigestSha256[0]) == -1 ) {
        return {error: "invalid sig: "+data.apkCertificateDigestSha256[0]+": not in "+allowedSigs, data}
    }
    if ( !data.ctsProfileMatch  || ! data.basicIntegrity ) {
        return { error: "Failed integrity check", data }
    }
    let nonceError = validateNonce(data, requireFreshAttestation, hmac)
    if (nonceError.error){
        return nonceError
    }
    try {
        ret = await axios.post( verify_url+api_key,{signedAttestation:orig} )
	if ( ret.data.isValidSignature!=true )
		return {error: "failed signature validation", data}

        return data

    } catch( e) {
	if ( !e.response )
		return {error: e.toString()}
	if ( e.response.status != 200 )
		return {error: "Http error "+e.response.status +" "+e.response.statusText }
        return {error:JSON.stringify(e.response.data)}
    }
}

var separator = "__$$__"
var millisPerDay = 24 * 60 * 60 * 1000

function createNonce(time) {
    if (time === undefined) {
        time = Date.now();
    }
    console.log("createNonce called at time:", time)
    if (!verySecretString){
        return {error: "Server has no secret"}
    }
    return Buffer.from(time + separator + sjcl.codec.hex.fromBits(sjcl.hash.sha256.hash(time + verySecretString))).toString('base64');
}

function validateNonce(data, requireFreshAttestation, hmac) {
    if (hmac != null && hmac != undefined) {
        let nonce_decoded = Buffer.from(data.nonce,'base64').toString()
        if (hmac === nonce_decoded) {
            console.log("Valid nonce, equals request HMAC")
            return true
        }
    }
    let nonce = Buffer.from(data.nonce, 'base64').toString()
    let parts = nonce.split(separator)
    if (parts.length != 2){
        console.log("Invalid nonce", nonce)
        return {error: "Invalid nonce", data}
    }
    if (Date.now() < parts[0]){
        console.log("Nonce is from the future")
        return false
    }

    if (requireFreshAttestation && Date.now() - parts[0] > millisPerDay){
        console.log("Nonce too old")
        return false
    }

    let hash = createNonce(parts[0])
    if (hash === data.nonce) {
        console.log("Valid nonce hash")
        return true;
    }
    console.log("Nonce is not good", nonce, hash)
    return false;
}

/*
//parse input file
file=process.argv[2] || 'safety.attestation'
orig = require('fs').readFileSync(file).toString()

(async()=>{
//	console.log(parseJwt(orig))
 	console.log(await validateJwt(orig))
})()
*/

module.exports={validateJwt, parseJwt, createNonce, validateNonce}
