#!/bin/bash

root=`dirname $0`

function usage {
cat <<EOF

usage: logs {cmd}
 logs {filename.zip} - decrypt all logs from zipfile (created with getZipLogsToSend() )
 logs {file.log} 	 - decrypt a single log file

adb commands (work with debug version only..) 
 logs last 			 - get latest log.log, and decode it to screen
 logs ls 			 - list the content of the logs folder on the device
 logs get {file.log} - get specific log file from device (from above {logs ls}
 logs getzip 		 - get the latest generated logs.zip from device.

EOF
exit 1
	
}

export CLASSPATH=$root/app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes

case "$1" in 

	*.zip)
		java com.tabookey.logs.cmd zip $1
		;;

	*.log) 
		java com.tabookey.logs.cmd read $1
		;;

	ls|list)
		adb shell run-as com.tabookey.bizpoc ls files/logs
		;;

	get)
		adb shell run-as com.tabookey.bizpoc cat files/logs/$2 > /tmp/log.log
		java com.tabookey.logs.cmd read /tmp/log.log
		;;

	getzip)
		adb shell run-as com.tabookey.bizpoc cat files/logs.zip > /tmp/logs.zip
		echo pulled logs to /tmp/logs.zip
		;;
		
	last) 
		adb shell run-as com.tabookey.bizpoc cat files/logs/log.log > /tmp/log.log
		java com.tabookey.logs.cmd read /tmp/log.log
		;;

	*) usage ;;
esac
