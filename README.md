# Jenkins Docker Swarm plugin

## Description
This plugin allows to add a Docker Swarm as a cloud agent provider. This allows to dynamically spin up single use Jenkins agents inside the Docker Swarm from a given Docker image. The creation is done with a [Docker Agent serviceSpec](https://docs.docker.com/engine/swarm/how-swarm-mode-works/services/) which allows options like Limits and Reservations to be set on agent containers.

## Configuration
The configuration options can be found at `/configure` (Configure System).

In the `Cloud` section, click `Add a new cloud` and select `Docker Swarm`. Then the connection to the swarm needs to be configured. Make sure that your swarm correctly exposes the API. The API is not exposed by default, so you will likely need to take manual actions to expose it. Depending on your OS the method may vary but you basically need to add the `-H tcp://0.0.0.0:<port>` option at Docker startup. Failing to do so will result in a `Failed to _ping` error. One way to to this is to create/edit the `daemon.json` file which could then look like the following on Windows when the API should be exposed on port 2375:
```
{
    "hosts": ["tcp://0.0.0.0:2375","npipe://"]
}
```

Next you need to add one or more agent templates. Make sure to set a `label`. A build requiring this label will run in a container spawned by the plugin. The default `command` downloads the agent jar and automatically connects to jenkins. In case of Windows containers, the flag in `Placement...` -> `Windows Container` should be set which overrides the command with the powershell version.

![configuration](/docs/images/configuration.png?raw=true "Configuration")

## Swarm Scheduling
The plugin attempts to create an agent as soon as build enters the queue. Bypassing cloud apis for faster agent scheduling.

## Caching
Caching is done via [docker volume plugin](https://github.com/suryagaddipati/docker-cache-volume-plugin).
Driver gets called to create an overlayfs cache volume for each build and once build is done volume gets deleted. This cache volume is mounted into agent in the directory specified by `Cache Dir` configuration option in Agent Templates.  On delete if there are any new changes to cache they get copied into a new basedir and pointer to baseCache gets updated. You can optionally mount lower base cache dir onto a NFS storage appliance. Checkout plugin documentation for more details.

## Swarm Dashboard
Follow the link `Docker Swarm Dashboard` on the sidebar to view the status of your swarm. It displays what build is executing where, what builds are in the queue for what resources ect.

![dashboard](/docs/images/dashboard.png?raw=true "Dashboard")
