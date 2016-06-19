# jenkins-docker-slaves

Launches a single use docker slave container from a given image on docker swarm.

This plugin does not use jenkin's cloud api for various reasons, main one being cloud api has a delay in provisioning and is not very well suited for single use throwaway slaves.

##Configuration 

![configuration](http://i.imgur.com/sxZJFi9.png "Configuration")
