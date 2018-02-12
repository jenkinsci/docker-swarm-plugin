# Jenkins docker swarm plugin

Launches a single use [docker agent serviceSpec](https://docs.docker.com/engine/swarm/how-swarm-mode-works/services/) from a given image on docker swarm.

This allows for serviceSpec options like Limits and Reservations to be set on agent containers.

### Configuration

![configuration](https://raw.githubusercontent.com/suryagaddipati/jenkins-docker-swarm-plugin/master/docs/images/configuration.png "Configuration")

### Swarm Scheduling

Plugin attempts to create an agent as soon as build enters the queue. Bypasses cloud apis for faster agent scheduling.

## caching
 Caching is done via [docker volume plugin](https://github.com/suryagaddipati/docker-cache-volume-plugin).
 Driver gets called to create an overlayfs cache volume  for each build and once build is done volume gets deleted. This cache volume is mounted into agent in the directory specified by `Cache Dir` configuration option in Agent Templates.  On delete if there are any new changes to cache they get copied into a new basedir and pointer to baseCache gets updated. You can optionally mount lower base cache dir onto a NFS storage appliance.  Checkout plugin documentation for more details.

### Swarm Dashboard

Follow the link on sidebar to view the status of your swarm. What is executing where, what builds are in queue for what resources ect

![dashboard](https://raw.githubusercontent.com/suryagaddipati/jenkins-docker-swarm-plugin/master/docs/images/dashboard.png "Dashboard")
