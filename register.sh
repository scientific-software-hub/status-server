CID=`docker ps -aqf "name=tango-cs"`
echo $CID
docker exec $CID /usr/local/bin/tango_admin --add-server StatusServer2/dev StatusServer2 dev/xenv/status_server
