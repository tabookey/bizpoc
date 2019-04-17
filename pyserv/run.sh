#!/bin/bash -e

dir=`dirname $0`

if [ ! -d $dir/venv ]; then

        cd $dir
        virtualenv venv
        source venv/bin/activate
        pip install flask flask-cors requests
fi

source $dir/venv/bin/activate
export FLASK_ENV=production
case "$1" in 
	"") flask run -h 0.0.0.0 ;;
	*) flask $*
esac

