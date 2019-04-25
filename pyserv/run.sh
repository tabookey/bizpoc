#!/bin/bash -e

dir=`dirname $0`

if [ ! -d $dir/venv ]; then

        cd $dir
        virtualenv venv
        source venv/bin/activate
        pip install flask flask-cors requests pyopenssl
fi

source $dir/venv/bin/activate
export FLASK_ENV=production
cd $dir
python app.py

