#!/bin/bash -e

dir=`dirname $0`
export LC_ALL=C
if [ ! -d $dir/venv ]; then

        cd $dir
        virtualenv venv
        source venv/bin/activate
        pip install flask flask-cors requests pyopenssl gunicorn
fi

source $dir/venv/bin/activate
export FLASK_ENV=production
cd $dir
gunicorn -b 0.0.0.0:443 --keyfile=certs/privkey.pem --certfile=certs/cert.pem app:app
