

provisioning server configuration:
Unlike the proxy server, this server is NOT long-running.
it does not automatically restart, and should manually be restarted in case the machine was restarted.

- server is deployed on prov-bizpoc.ddns.tabookey.com

- configure .ssh/config entry:
  host prov-bizpoc
        hostname prov-bizpoc.ddns.tabookey.com
        user ubuntu
        ForwardAgent yes

  host dprov-bizpoc
	hostname dprov-bizpoc.ddns.tabookey.com
        user ubuntu
        ForwardAgent yes

- initial setup:
	NOTE: ssh, scp hostname is taken from above sshconfig
	ssh dprov-bizpoc mkdir prov
	scp pyserv/run.sh pyserv/app.py prov-bizpoc:prov/
	ssh dprov-bizpoc

	HOSTNAME=dprov-bizpoc.ddns.tabookey.com	
	cd prov
	mkdir certs
	ln -s /etc/letsencrypt/live/$HOSTNAME/fullchain.pem certs/cert.pem
	ln -s /etc/letsencrypt/live/$HOSTNAME/privkey.pem certs/privkey.pem

- certificate created with certbot: MUST BE REPEATED MANUALLY every 2 months
	sudo apt-get update
	sudo apt-get install certbot
	sudo certbot certonly --standalone --preferred-challenges http -d $HOSTNAME

-install virtualenv
	sudo apt-get install virtualenv

run server: (initial runs installs the virtualenv and takes longer)
	sudo ./run.sh &
	#following commands "break the bridge", to make the production server inaccessible
	sudo pkill -f ssh 

run debug server:
	./debug.sh (more relevant on local dev machine: it runs on port 8080, which is inaccessible from the internet)

show flask routes:
	./debug.sh routes

sample run:

	#put entry
	#NOTE: no suffix in debug is "valid" otp. "-error" (or any other word) is invalid otp
	#specifically cccccckftlhc is valid "admin" otp (mine...)
	#next param is user's otp. only id (12 chars) are used, its not verified by this call
	# encrypted data - the server doesn't care. here we put "123"
curl -H 'Content-Type:application/json' http://localhost:8080/putEncryptedCredentials/cccccckftlhc/12345678901234/mychecksum -d '{"encryptedCredentials":"123"}'

	#show in-memory entries (debug server only...)
curl http://localhost:8080/dump

	#attempt get item (fail on OTP check)
curl http://localhost:8080/getEncryptedCredentials/12345678901234-fail/asdasd

	#failed get: OTP ok, but wrong checksum
curl http://localhost:8080/getEncryptedCredentials/12345678901234/asdasd

	#success
curl http://localhost:8080/getEncryptedCredentials/12345678901234/mychecksum

try dump, get again, to see that nothing is there..


