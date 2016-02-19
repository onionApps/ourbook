echo packing ...
tar -cf bin.tar tor_arm tor_arm_old tor_x86 tor_x86_old
echo compressing ...
lzma -k -9 -f bin.tar #3.9mb
#xz --format=lzma -k -f -9 -e --memory=80MiB bin.tar
#xz -k -f -9 -e --lzma2=dict=512MiB,nice=273  bin.tar
wc -c bin.tar
wc -c bin.tar.lzma
#bzip2 -k -9 -f bin.tar
#xz -k -9 -f bin.tar
echo writing resource ...
cp bin.tar.lzma ../app/src/main/res/raw/bin.mp3
echo deleting temp ...
rm bin.tar
echo ready.
