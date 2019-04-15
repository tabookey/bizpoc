#!/bin/bash -e

dir=`dirname $0`
source $dir/venv/bin/activate
export FLASK_ENV=production
flask ${1:run}
