#!/bin/bash -e
if ! [ $(id -u) = 0 ]; then
   echo "Must be run as root."
   exit 1
fi
set -x
rootdir="`pwd`"
squashdir="squashdir"
tmpsquashdir="squashdir2"
bitkeydir="${rootdir}/bitkey"
squashfs="${bitkeydir}/casper/10root.squashfs"
newsquashfs="20squashfs"
bizpocfile="bizpoc.html"
bitkeyiso="turnkey-bitkey-14.2.0-jessie-amd64.iso"
tmpisodir="tmpiso"
# clean up
rm ${tmpsquashdir} -rf
rm ${newsquashfs} -rf
rm ${bitkeydir} -rf

mkdir -p ${tmpisodir}
sha256sum ${bitkeyiso}
echo 7083fcfd945eb0c5394ada8261c8054da28122e6b18783897063127fa2f4cc0d expected
mount ${bitkeyiso} ${tmpisodir}
cp ${tmpisodir} ${bitkeydir} -a
umount ${tmpisodir}
mount ${squashfs} ${squashdir}
cp ${squashdir} ${tmpsquashdir} -a
umount ${squashdir}

cd ${rootdir}/../..
rm -f ${bizpocfile}
./mergehtml bizpoc-prov.html ${bizpocfile} 
cd ${rootdir}
mv ../../${bizpocfile} ${tmpsquashdir}/home/user/${bizpocfile}
mkdir -p ${tmpsquashdir}/home/user/keygen/  
cp ../src ${tmpsquashdir}/home/user/keygen/ -a
cp envsetup ${tmpsquashdir}/home/user/keygen/
cp node_modules.tar.gz ${tmpsquashdir}/home/user/keygen/
cp node-v10.16.0-linux-x64.tar.xz ${tmpsquashdir}/home/user/keygen/

set +x
while [ "${ok}" != "ok" ] ; do
    read -p "Enter ok once done editting squashfs in ${tmpsquashdir}: " ok
    sleep 3;
done
mksquashfs ${tmpsquashdir} ${newsquashfs} 
set -x
cp ${newsquashfs} ${squashfs}
cd ${bitkeydir} 
genisoimage -V LAZYALEX${1} -no-emul-boot -boot-load-size 4 -boot-info-table -b isolinux/isolinux.bin -c isolinux/isolinux.cat -r -J -o ${rootdir}/bitkey${1}.iso ./
cd ${rootdir}
isohybrid bitkey${1}.iso 
usbdevice=`mount|grep LAZYALEX|cut -d " " -f 1`
[ -b "${usbdevice}" ]
dd if=bitkey${1}.iso of="${usbdevice}" && sync
set +x
echo done
