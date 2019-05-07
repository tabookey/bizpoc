from flask import Flask, request,jsonify
from flask_cors import CORS
import requests
import re

app = Flask(__name__)
CORS(app)

class Data:
    def __init__(self, creds,checksum):
        self.creds=creds
        self.checksum=checksum

         # 123456789012
masters=[ "cccccckftlhc", #dror's
          "cccccckftivv", #alex's
	  "cccccceefukg", #liraz's
        ]
app.data=dict()

app.YUBI_API='https://api2.yubico.com/wsapi/2.0/verify?id=1&nonce=1234567890123456&otp='

app.VERIFY_SAFETYNET='https://bizpoc.ddns.tabookey.com/safetynet/'

app.allowReplay=False

@app.errorhandler(AssertionError)
def handle_assertion_error(error):
    return jsonify(name="failed", error=str(error)), 400

def verify(otp,name):
    c=requests.get(app.YUBI_API+otp ).content
    searchres=re.search( r"\n(?:status=(\S+))", c)
    res = searchres.group(1) if searchres else "no-status" + app.YUBI_API
    assert res=='OK' or ( app.allowReplay and res=='REPLAYED_REQUEST' ), "OTP "+name+" failed:" + res

@app.route('/checkYubikeyExists/<otpid>')
def checkYubikeyExists1(otpid):
    assert app.data.get(otpid[:12]), "unknown"
    return jsonify( result="ok" )

@app.route('/checkYubikeyExists/<otpid>/<checksum>')
def checkYubikeyExists(otpid, checksum):
    d = app.data.get(otpid[:12])
    assert d and d.checksum == checksum, "Invalid otp/checksum"
    return jsonify( result="ok" )

@app.route('/getEncryptedCredentials/<otp>/<checksum>')
def getEncryptedCredentials(otp,checksum):
    id=otp[:12]
    checkYubikeyExists(otp,checksum)
    verify(otp, "user") #verify only after validating checksum: avoid "POP" if checksum doesn't match

    safetyres=requests.get( app.VERIFY_SAFETYNET+str(request.headers.get('x-safetynet')) )
    assert safetyres.status_code == 200 and safetyres.content.nonce, "failed to verify safetynet: "+str(safetyres.content)
    return jsonify( encryptedCredentials=app.data.pop(id).creds )

@app.route("/putEncryptedCredentials/<masterotp>/<otpid>/<checksum>", methods=['POST'])
def putEncryptedCredentials(masterotp,otpid, checksum):
    assert masterotp[:12] in masters, "Invalid master OTP" #validate master is in the list
    verify(masterotp, "master")
    #not validating client otp: we trust masterotp
    app.data[ otpid[:12] ] = Data(request.get_json()["encryptedCredentials"] ,checksum)
    return jsonify( result="put" )

if __name__ == "__main__":
	app.run(host='0.0.0.0', port=443, ssl_context=('certs/cert.pem', 'certs/privkey.pem'))
