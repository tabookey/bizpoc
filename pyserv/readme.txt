One-time configuraiton:

1. Create environment for server:
	virtualenv venv
2. start env: 
	source ./venv/bin/activate
3. install apps required package:
	pip install flask

run server:
	./run.sh run
run debug server:
	./debug.sh run

to show configured server paths, use "./debug.sh routes"

sample run:

	#put entry
	#NOTE: -OK is debug suffix for "valid" otp. anything else is "invalid" otp
	#specifically cccccckftlhc is valid "admin" otp (mine...)
	#next param is user's otp. only id (12 chars) are used, its not verified by this call
	# encrypted data - the server doesn't care. here we put "123"
curl -H localhost:5000'Content-Type:application/json' http://localhost:5000/putEncryptedCredentials/cccccckftlhc-OK/12345678901234/mychecksum -d '{"encryptedCredentials":"123"}'

	#show in-memory entries:
curl http://localhost:5000/dump

	#attempt get item (fail on OTP check)
curl http://localhost:5000/getEncryptedCredentials/12345678901234/asdasd

	#failed get: OTP ok, but wrong checksum
curl http://localhost:5000/getEncryptedCredentials/12345678901234-OK/asdasd

	#success
curl http://localhost:5000/getEncryptedCredentials/12345678901234-OK/mychecksum

try dump, get again, to see that nothing is there..


