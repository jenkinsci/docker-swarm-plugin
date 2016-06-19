# jenkins-docker-slaves

Launches a single use docker slave container from a given image on docker swarm. This plugin was originally based on the work done [here](https://github.com/Dockins/docker-slaves-plugin). 

This plugin does not use jenkin's cloud api for various reasons, main one being cloud api has a delay in provisioning and is not very well suited for single use throwaway slaves.

##Configuration 

![configuration](http://i.imgur.com/sd5kVSr.png "Configuration")

#### optional caching 
if you would like you use build caching. Compile and launch [this volume driver] (https://github.com/suryagaddipati/jenkins-docker-swarm-plugin/tree/master/src/main/golang/docker-cache-driver). This driver is written to use overlayfs but anyother CoW should work in theory ( PR's welcome :)).

## Swarm Scheduling 

Plugin attempts to create a slave as soon as build enters the queue. And if requested cpus/memory is not availabe on swarm, it would keep retrying( on a timer) until resources are availble.

## caching 
 Caching is done via [docker volume plugin](https://github.com/suryagaddipati/jenkins-docker-swarm-plugin/tree/master/src/main/golang/docker-cache-driver) . 
 Driver gets called to create an overlayfs cache volume  for each build and once build is done volume gets delted. On delete if there are any new changes to cache they get copied into a new basedir and pointer to baseCache gets updated. You can optionally mount lower base cache dir onto a NFS storage appliance. 
 

