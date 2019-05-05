#!/usr/bin/env node
const express = require('express')
const proxy = require('http-proxy-middleware')
var cors = require('cors')

jwtverify = require('./jwtverify')

const greenlock_express = require('greenlock-express')
const url = require('url')
const fs = eval("require('fs')")

if ( !process.argv[2] ) {
	console.log( "usage: proxy.js {selfhostname}\n" +
		" - register greenlock on given server\n"+
		" - forward requests to www.bitgo.com\n"+
		" - forward port 8090 to test.bitgo.com\n" +
		" proxy.js -d\n" +
		" - debug mode: only port 8090 to test\n"

	)
	process.exit(1)
}

var hostname = process.argv[2]
if ( hostname == "-d" ) {
    var target = process.argv[3] || "https://test.bitgo.com"
    console.log("Debug mode")
    createServer( {port:8090, target} )
    return
}

var certsDir = "certs"
var greenlock_server=null

createServer( {https:true, port:443, greenlock:true, target:"https://www.bitgo.com"} )
createServer( {https:true, port:8090, greenlock:false, target:"https://test.bitgo.com"} )

function createServer({https, port, greenlock, target}) {
server = express()

server.use(cors())
server.use((req, res, next) => {
    delete req.headers["origin"]
    next();
});

myProxy=proxy( {target, changeOrigin:true, logLevel:"debug" })

//getKey requests must have safetynet header
function validateSafetynet(req,res,next) {
    //bitgo requests that require "x-safetynet" header 
    let requestsWithSafetyNet= '/api/v2/\\w+/(key|wallet/)'

    if (!req.originalUrl.match(requestsWithSafetyNet) ){
        //other requests are passed-through.
        next()
	return
    }

    header = req.headers["x-safetynet"]
    if ( !header ) {
        res.send("X-Safetynet header is missing for "+req.originalUrl+"\n").status(400)
	return
    }
    jwtverify.validateJwt(header).then(res=>{
        if ( res.error ) { 
            throw new Error(res.error)
        }
        next()
    }).catch( err=> {
        res.send("Safetynet failure: "+err+"\n").status(400)
    })

}

//wrapper for safetynet check. used by the provisioning server.
server.get( "/safetynet/:jwt", (req,res) => {
    jwtverify.validateJwt(req.params.jwt)
        .then(ret=>{
            res.send(ret)
        })
        .catch(err=>{
            res.send(err.toString(),400)
        })
})

// server.use( '/api/v2/teth/key/', validateSafetynet, myProxy )
server.use( '/api', validateSafetynet, myProxy )

if ( !greenlock ) {
    var keyfile = "certs/live/"+hostname+"/privkey.pem"
    var certfile = "certs/live/"+hostname+"/fullchain.pem"

	if ( https ) {
        counter=5
        //secondary https server. don't run greenlock, but rely on its generated certs:
		function createServer1() {
            if ( !fs.existsSync(certfile) ) {
                if ( !greenlock_server || !counter ) {
                    console.log( "FATAL: no greenlock to create cert")
                    process.exit(1)
                }
                counter--
                console.log( "no "+keyfile+". waiting for greenlock to create ")
                setTimeout(createServer1,3000)
                return
            }
        	var key  = fs.readFileSync(keyfile, 'utf8');
        	var cert = fs.readFileSync(certfile, 'utf8');

        	require('https').createServer({ key, cert },server).listen(port)
            console.log( "Listening on port ", port, "forwarding to ", target)
		}

        createServer1()
    } else {
        require('http').createServer(server).listen(port)
        console.log( "Listening on port ", port, "forwarding to ", target)
    }

} else { //greenlock - https
    try { fs.mkdirSync(certsDir) } catch (e) {}
    greenlock_server = server = greenlock_express.create({
            approvedDomains: [hostname],
            version: 'draft-11',
            server: 'https://acme-v02.api.letsencrypt.org/directory',
            email: "greenlock.user@gmail.com",
            agreeTos: true,
            configDir: certsDir,
            communityMember: false,
            app: server,
            //debug: true,
        })
    server.listen(80,port)
    console.log( "Listening on port ", port, "forwarding to ", target)
} 


}
