#!/bin/bash -e

dir=`dirname $0`
export LC_ALL=C
if [ ! -d $dir/venv ]; then

	cd $dir
	virtualenv venv
	source venv/bin/activate
	pip install flask flask-cors requests gunicorn
fi

source $dir/venv/bin/activate
export FLASK_APP=debug
export FLASK_ENV=development
case "$1" in 
	"") gunicorn -b 0.0.0.0:443 --keyfile=certs/privkey.pem --certfile=certs/cert.pem debug:app ;;
	*) flask $*
esac

