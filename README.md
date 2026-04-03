# Jenkins Docker Swarm plugin
[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/docker-swarm-plugin/master)](https://ci.jenkins.io/job/Plugins/job/docker-swarm/job/master/)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/docker-swarm.svg)](https://plugins.jenkins.io/docker-swarm)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/docker-swarm-plugin.svg?label=release)](https://github.com/jenkinsci/docker-swarm-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/docker-swarm.svg?color=blue)](https://plugins.jenkins.io/docker-swarm)

## Description
This plugin allows to add a Docker Swarm (Swarm Mode) as a cloud agent provider. This allows to dynamically spin up single use Jenkins agents inside the Docker Swarm from a given Docker image. The creation is done with a [Docker Agent serviceSpec](https://docs.docker.com/engine/swarm/how-swarm-mode-works/services/) which allows options like Limits and Reservations to be set on agent containers.

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
There are 2 options:
1. **Caching is done via [docker volume plugin](https://github.com/suryagaddipati/docker-cache-volume-plugin).**
Driver gets called to create an overlayfs cache volume for each build and once build is done volume gets deleted. This cache volume is mounted into agent in the directory specified by `Cache Dir` configuration option in Agent Templates.  On delete if there are any new changes to cache they get copied into a new basedir and pointer to baseCache gets updated. You can optionally mount lower base cache dir onto a NFS storage appliance. Checkout plugin documentation for more details.
1. **Caching is done via the default docker volume driver.** To activate it, type `local` into the `Cache driver name` field in the docker swarm cloud configuration.
Watch out for concurrent access as it is possible for 2 containers to access the same cache at the same time. This is not an issue if you use it as a repository cache when controlled by git or mercurial. They have internal mechanisms for serializing access.

## Swarm Dashboard
Follow the link `Docker Swarm Dashboard` on the sidebar to view the status of your swarm. It displays what build is executing where, what builds are in the queue for what resources ect.

![dashboard](/docs/images/dashboard.png?raw=true "Dashboard")

## Development Notes
### Jenkins Plugin Documentation
https://jenkins.io/doc/developer/publishing/releasing/

### Local build, test and package
```
mvn compile
mvn test
mvn package
```

### Prepare the release locally and perform the release
```
mvn release:prepare
mvn release:perform
```

### Create Release Notes
After the release, create a new release on GitHub based on the already present tag.
Fill in the new features and bugfixes and publish the release.
