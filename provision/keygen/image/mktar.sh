#!/bin/bash -e
rootdir="`pwd`"
tmptardir="tardir"
bizpocfile="bizpoc.html"

rm -rf ${tmptardir}
cd ${rootdir}/../..
rm -f ${bizpocfile}

./mergehtml bizpoc-prov.html ${bizpocfile}
cd ${rootdir}
mkdir -p ${tmptardir}/keygen/
mv ../../${bizpocfile} ${tmptardir}/${bizpocfile}
cp ../src ${tmptardir}/keygen/ -a
cp node_modules.tar.gz ${tmptardir}/keygen/
cp node-v10.16.0-linux-x64.tar.xz ${tmptardir}/keygen/
tar -czvpf keygen.tar.gz -C ${tmptardir} .
rm -rf ${tmptardir}
