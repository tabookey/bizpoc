from app import app,verify,data,jsonify

host="http://localhost:5000/"
app.YUBI_API=host+"mock_verify/"
app.allowReplay=True

#DEBUG-ONLY: dump current repo content (even in debug, only dump cred len, not content)
@app.route( "/dump")
def debug_dump():
   return jsonify( [ dict(key=d, cs=data[d].checksum, creds=data[d].creds) for d in data ] )

@yubimock.route( "/mock_verify/<otp>")
def dummy_verify(otp):
    stat = re.search( r"-(\w+)", otp).group(1) or "FAILED"
    return "\nstatus="+stat+"\n"
