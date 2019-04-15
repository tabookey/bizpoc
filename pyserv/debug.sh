#!/bin/bash -e

dir=`dirname $0`
source $dir/venv/bin/activate
export FLASK_APP=debug
export FLASK_ENV=development
flask ${1:run}
