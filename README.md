# Jenkins docker swarm plugin

Launches a single use [docker agent service](https://docs.docker.com/engine/swarm/how-swarm-mode-works/services/) from a given image on docker swarm.

This allows for service options like Limits and Reservations to be set on agent containers.

### Configuration

![configuration](http://i.imgur.com/sd5kVSr.png "Configuration")

#### Add this 'cloud' for label completion
![label](http://i.imgur.com/IfyzNW7.png)

#### optional caching
if you would like you use build caching. Compile and launch [this volume driver] (https://github.com/suryagaddipati/jenkins-docker-swarm-plugin/tree/master/src/main/golang/docker-cache-driver). This driver is written to use overlayfs but anyother CoW should work in theory ( PR's welcome :)).

## Swarm Scheduling

Plugin attempts to create a slave as soon as build enters the queue. And if requested cpus/memory is not availabe on swarm, it would keep retrying( on a timer) until resources are availble.

## caching
 Caching is done via [docker volume plugin](https://github.com/suryagaddipati/jenkins-docker-swarm-plugin/tree/master/src/main/golang/docker-cache-driver) .
 Driver gets called to create an overlayfs cache volume  for each build and once build is done volume gets delted. On delete if there are any new changes to cache they get copied into a new basedir and pointer to baseCache gets updated. You can optionally mount lower base cache dir onto a NFS storage appliance.

## Swarm Dashboard

Follow the link on sidebar to view the status of your swarm. What is executing where, what builds are in queue for what resources ect

![dashboard](http://i.imgur.com/A4Ltqkh.png "Dashboard")
