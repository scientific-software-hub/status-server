CID=`docker ps -aqf "name=tango-cs"`
echo $CID
docker exec $CID /usr/bin/tango_admin --add-server StatusServer2/test StatusServer2 test/status_server/test
