import thread
import re
from app import app
import requests
from flask import Flask
import pytest

yubimock=Flask("yubimock")
@yubimock.route( "/mock_verify/<otp>")
def dummy_verify(otp):
    stat = re.search( r"-(\w+)", otp).group(1) or "FAILED"
    return "\nstatus="+stat+"\n"

def runMock():
    yubimock.run(port=12345, debug=False)
   
thread.start_new_thread(runMock,())

app.YUBI_API="http://localhost:12345/mock_verify/"


@pytest.fixture
def client():
    app.config['TESTING'] = True
    client = app.test_client()
    yield client

#test our mock: suffix of "-OK" is valid otp, -REPLAYED_REQUEST is valid, replay OTP and anything else is failure 
def test_mock(client):
    with pytest.raises(AssertionError):
        app.verifyOtp("asdasd")
    app.verifyOtp("otp-OK")
    with pytest.raises(AssertionError):
        app.verifyOtp("otp-REPLAYED_REQUEST")

def test_mock_replay(client):
    #allowReplay is valid ony for replay, not other errors.
    with pytest.raises(AssertionError):
        app.verifyOtp("otp-ERROR", True)

    app.verifyOtp("otp-REPLAYED_REQUEST", True)



