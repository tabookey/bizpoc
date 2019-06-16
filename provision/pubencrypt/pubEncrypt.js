crypt = require( 'public-encrypt' )

global.pubEncrypt = function( key,str ) {

	return crypt.publicEncrypt( key, Buffer.from(str) ).toString( "base64" )

}

//todo: why this exports is invisible from <script> ?
module.exports= {
	publicEncrypt : crypt.publicEncrypt
}

