#!/bin/bash
pid=`jps -mlv | grep StackAnalysis-1.0-SNAPSHOT.jar|awk 'NR==1' | awk '{print $1}'`;
if [ -n "$pid" ];then
  kill -9 $pid;
  echo "Process stopped successfully";
else
  echo "No running processes found";
fi