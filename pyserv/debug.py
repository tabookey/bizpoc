from flask import Flask
import re
from app import app,verify,jsonify

app.allowReplay=True
#app.YUBI_API="http://localhost:6000/mock_verify/"
app.YUBI_API="https://prov-bizpoc.ddns.tabookey.com/mock_verify/"

#yubimock=Flask("yubimock")

#DEBUG-ONLY: dump current repo content (even in debug, only dump cred len, not content)
@app.route( "/dump")
def debug_dump():
   return jsonify( [ dict(key=d, cs=app.data[d].checksum, creds=app.data[d].creds) for d in app.data ] )

@app.route( "/mock_verify/<otp>")
def dummy_verify(otp):
    stat = re.search( "(?:-([\\w]*))?$", otp).group(1) or "OK"
    return "\nstatus="+stat+"\n"



if __name__ == "__main__":
	app.run(host='0.0.0.0', port=443, ssl_context=('certs/cert.pem', 'certs/privkey.pem'))
