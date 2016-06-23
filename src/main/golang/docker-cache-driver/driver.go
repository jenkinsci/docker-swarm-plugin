package main

import (
	"errors"
	"fmt"
	"github.com/docker/go-plugins-helpers/volume"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
)

const cacheLowerRootDir = "/cache"
const cacheUpperRootDir = "/mnt/cache-upper"
const cacheWorkRootDir = "/mnt/cache-work"
const cacheMergedRootDir = "/mnt/cache-merged"

type cacheDriver struct {
	mutex *sync.Mutex
	name  string
}

func newCacheDriverDriver() cacheDriver {
	fmt.Println("Starting... ")
	_, _ = newCacheState() //handle error here
	driver := cacheDriver{
		mutex: &sync.Mutex{},
		name:  "cache-driver",
	}
	return driver
}

func (driver cacheDriver) Get(req volume.Request) volume.Response {
	fmt.Println("Get Called... ")

	jobName, buildNumber, err := getNames(req.Name)
	buildCache, _ := newBuildCache(jobName, buildNumber)
	if err != nil {
		return volume.Response{Err: fmt.Sprintf("The volume name %s is invalid.", req.Name)}
	}
	if buildCache.exists() {
		return volume.Response{
			Volume: driver.volume(jobName, buildNumber),
		}
	}

	return volume.Response{
		Err: fmt.Sprintf("No volume found with the name %s", req.Name),
	}
}

func (driver cacheDriver) List(req volume.Request) volume.Response {
	fmt.Println("List Called... ")
	matches, err := filepath.Glob(fmt.Sprintf("%s/*/*", cacheMergedRootDir))
	if err != nil {
		return volume.Response{Err: fmt.Sprintf("Couldn't glob cache dir %s due to %s", cacheMergedRootDir, err)}
	}
	if matches != nil {
		var volumes []*volume.Volume = make([]*volume.Volume, len(matches))
		for i, match := range matches {
			mergeDir := strings.Replace(match, cacheMergedRootDir+"/", "", -1)
			dirs := strings.Split(mergeDir, "/")
			volumes[i] = driver.volume(dirs[0], dirs[1])
		}
		fmt.Printf("Found %s volumes\n", strconv.Itoa(len(volumes)))
		return volume.Response{
			Volumes: volumes,
		}
	}

	return volume.Response{}

}

func (driver cacheDriver) Create(req volume.Request) volume.Response {
	fmt.Println("Create Called :", req.Name)
	driver.mutex.Lock()
	defer driver.mutex.Unlock()
	jobName, buildNumber, err := getNames(req.Name)
	if err != nil {
		return volume.Response{Err: fmt.Sprintf("The volume name %s is invalid.", req.Name)}
	}
	buildCache, _ := newBuildCache(jobName, buildNumber)
	fmt.Println("checking if exists", getMergedPath(jobName, buildNumber))
	if buildCache.exists() {
		return volume.Response{Err: fmt.Sprintf("The volume %s already exists", req.Name)}
	}

	buildCache.initDirs()
	cacheState, err := newCacheState()
	baseBuildDir, err := cacheState.baseBuildDir(jobName)
	err = buildCache.mount(baseBuildDir)
	if err != nil {
		return volume.Response{Err: fmt.Sprintf("Failed to mount overlay cache due to  %s", err)}
	}

	return volume.Response{}
}

func (driver cacheDriver) Remove(req volume.Request) volume.Response {
	fmt.Print("Remove Called... ")
	driver.mutex.Lock()
	defer driver.mutex.Unlock()

	jobName, buildNumber, err := getNames(req.Name)
	if err != nil {
		return volume.Response{Err: fmt.Sprintf("The volume name %s is invalid.", req.Name)}
	}
	buildCache, _ := newBuildCache(jobName, buildNumber)
	err = buildCache.destroy()
	if err != nil {
		return volume.Response{Err: fmt.Sprintf("Failed to destory volume : %s", err)}
	}

	return volume.Response{}
}

func (driver cacheDriver) Path(req volume.Request) volume.Response {

	jobName, buildNumber, err := getNames(req.Name)
	if err != nil {
		return volume.Response{Err: fmt.Sprintf("The volume name %s is invalid.", req.Name)}
	}
	fmt.Println("Path called with ", jobName, " ", buildNumber)
	return volume.Response{Mountpoint: getMergedPath(jobName, buildNumber)}
}

func (driver cacheDriver) Mount(req volume.Request) volume.Response {
	return driver.Path(req)
}
func (driver cacheDriver) Unmount(req volume.Request) volume.Response {
	driver.mutex.Lock()
	defer driver.mutex.Unlock()
	fmt.Printf("Unmounted %s\n", req.Name)

	return driver.Path(req)
}

func (driver cacheDriver) volume(jobName, name string) *volume.Volume {
	return &volume.Volume{
		Name:       jobName + "-" + name,
		Mountpoint: getMergedPath(jobName, name),
	}
}

func getNames(volumeName string) (string, string, error) {
	names := strings.Split(volumeName, "-")
	if len(names) > 1 {
		return names[0], names[1], nil
	}
	return "", "", errors.New(volumeName + " is not valid.")
}
