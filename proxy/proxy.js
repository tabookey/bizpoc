const express = require('express')
const proxy = require('http-proxy-middleware')
 
const greenlock_express = require('greenlock-express')
const url = require('url')
const fs = eval("require('fs')")

var port = parseInt(process.argv[2]) || 8080
var hostname = process.argv[3]

var certsDir = "certs"

var target = process.argv[4] || 'https://www.bitgo.com'
server = express()
server.use( '/api', proxy( {target, changeOrigin:true, logLevel:"debug" }))

if ( port==443 ) {
    try { fs.mkdirSync(certsDir) } catch (e) {}
    server = greenlock_express.create({
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
    server.listen(80,443)
} else {
    server.listen(port)
}

console.log( "Listening on port ", port, "forwarding to ", target)
