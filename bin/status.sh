#!/bin/bash
pid=`jps -mlv | grep StackAnalysis-1.0-SNAPSHOT.jar|awk 'NR==1' | awk '{print $1}'`;
if [ -n "$pid" ];then
  echo "Process is running";
  exit $pid;
else
  echo "Process stopped";
  exit 0;
fi