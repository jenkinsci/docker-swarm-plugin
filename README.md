# Jenkins docker swarm plugin

Launches a single use [docker agent serviceSpec](https://docs.docker.com/engine/swarm/how-swarm-mode-works/services/) from a given image on docker swarm.

This allows for serviceSpec options like Limits and Reservations to be set on agent containers.

## Configuration
The configuration options can be found at `/configure`
![configuration](https://raw.githubusercontent.com/jenkinsci/docker-swarm-plugin/master/docs/images/configuration.png "Configuration")

- Docker swarm api url: The URL to the API of the Swarm you want to target. The API is not exposed by default, so you will likely need to take manual actions to expose it. Depending on your OS the method may vary but you basically need to add the `-H tcp://0.0.0.0:<port>` option at Docker startup. Failing to do so will result in a "Failed to _ping" error.
- Label: the label to set on the agent (which will be a docker container). A build requiring this label will run in a container spawned by the plugin.

## Swarm Scheduling

Plugin attempts to create an agent as soon as build enters the queue. Bypasses cloud apis for faster agent scheduling.

## caching
 Caching is done via [docker volume plugin](https://github.com/suryagaddipati/docker-cache-volume-plugin).
 Driver gets called to create an overlayfs cache volume for each build and once build is done volume gets deleted. This cache volume is mounted into agent in the directory specified by `Cache Dir` configuration option in Agent Templates.  On delete if there are any new changes to cache they get copied into a new basedir and pointer to baseCache gets updated. You can optionally mount lower base cache dir onto a NFS storage appliance.  Checkout plugin documentation for more details.

## Swarm Dashboard

Follow the link on sidebar to view the status of your swarm. What is executing where, what builds are in queue for what resources ect

![dashboard](/docs/images/dashboard.png?raw=true "Dashboard")
