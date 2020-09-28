#!/bin/bash
baseDirParent=$(dirname $(readlink -f "$0"))
baseDir=$(dirname $baseDirParent)
pid=`jps -mlv | grep StackAnalysis-1.0-SNAPSHOT.jar|awk 'NR==1' | awk '{print $1}'`;
if [ -n "$pid" ];then
  echo "Process is running";
else
  nohup java -Xms2g -Xmx4g -jar $baseDir/StackAnalysis-1.0-SNAPSHOT.jar > /dev/null &
  echo "Process started successfully";
fi