#!/bin/bash -e

host=${host="dprov-bizpoc"}

echo upload to: $host
echo -n Press enter to continue.
read

scp run.sh app.py $host:prov/
ssh $host sudo -s ./start.sh || echo "start exit with $?"

echo "== running ssh $host tail -f ./prov/logfile"
echo "== you can press Ctrl-C (the server keeps running)"
echo ""
ssh $host tail -f ./prov/logfile.txt
