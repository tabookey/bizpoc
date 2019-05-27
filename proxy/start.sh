#!/bin/bash -xe

cd `dirname \`realpath $0\``
hostname=bizpoc.ddns.tabookey.com
/usr/bin/node proxy.js $hostname 
