
axios = require('axios')

verify_url = "https://www.googleapis.com/androidcheck/v1/attestations/verify?key="
api_key = "AIzaSyCz-RqbUmqaKhWqU12-38mTwXMNsV3rlfE"

//app signers we trust:
allowedSigs=[
	'TwsoRSoLU3adBbfTtNAqv8eARKTj4dsxMxxCsyabv8A='
]
allowedPackageNames=[
    'com.tabookey.bizpoc',
    'com.example.android.safetynetsample',
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

async function validateJwt(orig) {
    data = parseJwt(orig)
    if ( data.apkPackageName )
    if ( allowedPackageNames.indexOf(data.apkPackageName) == -1 ) {
        return {error: "invalid pkg: "+data.apkPackageName, data}
    }
    if ( allowedSigs.indexOf(data.apkCertificateDigestSha256[0]) == -1 ) {
        return {error: "invalid sig: "+data.apkCertificateDigestSha256[0], data}
    }
    if ( !data.ctsProfileMatch  || ! data.basicIntegrity ) {
        return { error: "Failed integrity check", data }
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

/*
//parse input file
file=process.argv[2] || 'safety.attestation'
orig = require('fs').readFileSync(file).toString()

(async()=>{
//	console.log(parseJwt(orig))
 	console.log(await validateJwt(orig))
})()
*/

module.exports={validateJwt, parseJwt}
