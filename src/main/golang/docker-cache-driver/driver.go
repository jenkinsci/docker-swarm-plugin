package main

import (
	"errors"
	"fmt"
	"github.com/docker/go-plugins-helpers/volume"
	"os"
	"path/filepath"
	"strings"
	"sync"
)

type cacheLocations struct {
	cacheLowerRootDir  string
	cacheUpperRootDir  string
	cacheWorkRootDir   string
	cacheMergedRootDir string
}

func newCacheLocations(cacheLowerRootDir, cacheUpperRootDir, cacheWorkRootDir, cacheMergedRootDir *string) cacheLocations {
	cacheLocations := cacheLocations{}
	cacheLocations.cacheLowerRootDir = *cacheLowerRootDir
	cacheLocations.cacheUpperRootDir = *cacheUpperRootDir
	cacheLocations.cacheWorkRootDir = *cacheWorkRootDir
	cacheLocations.cacheMergedRootDir = *cacheMergedRootDir
	return cacheLocations
}

type cacheDriver struct {
	mutex          *sync.Mutex
	name           string
	cacheLocations *cacheLocations
}

func newCacheDriverDriver(cacheLocations *cacheLocations) cacheDriver {
	fmt.Println("Starting Cache Driver... ")
	driver := cacheDriver{
		mutex:          &sync.Mutex{},
		name:           "cache-driver",
		cacheLocations: cacheLocations,
	}
	os.MkdirAll(driver.cacheLocations.cacheLowerRootDir, 0755)
	os.MkdirAll(driver.cacheLocations.cacheUpperRootDir, 0755)
	os.MkdirAll(driver.cacheLocations.cacheWorkRootDir, 0755)
	os.MkdirAll(driver.cacheLocations.cacheMergedRootDir, 0755)
	_, _ = newCacheState(driver.cacheLocations.cacheLowerRootDir) //handle error here
	return driver
}

func (driver cacheDriver) Get(req volume.Request) volume.Response {
	jobName, buildNumber, err := getNames(req.Name)
	buildCache := newBuildCache(jobName, buildNumber, driver.cacheLocations)
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
	cacheMergedRootDir := driver.cacheLocations.cacheMergedRootDir
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
		return volume.Response{
			Volumes: volumes,
		}
	}

	return volume.Response{}

}

func (driver cacheDriver) Create(req volume.Request) volume.Response {
	fmt.Println(fmt.Sprintf("Create-%s: Create Called.", req.Name))
	driver.mutex.Lock()
	defer driver.mutex.Unlock()
	jobName, buildNumber, err := getNames(req.Name)
	if err != nil {
		return volumeErrorResponse(fmt.Sprintf("Create-%s: The volume name is invalid.", req.Name))
	}

	buildCache := newBuildCache(jobName, buildNumber, driver.cacheLocations)
	if buildCache.exists() {
		return volumeErrorResponse(fmt.Sprintf("Create-%s: The volume already exists", req.Name))
	}

	fmt.Println(fmt.Sprintf("Create-%s: Creating dirs for the volume.", req.Name))
	if err := buildCache.init(); err != nil {
		return volumeErrorResponse(fmt.Sprintf("Create-%s: Failed to create Dirs. %s", req.Name, err))
	}
	fmt.Println(fmt.Sprintf("Create-%s: Volume Created!!", req.Name))

	return volume.Response{}
}

func (driver cacheDriver) Mount(req volume.Request) volume.Response {
	fmt.Println(fmt.Sprintf("Mount-%s: mount callled..", req.Name))
	driver.mutex.Lock()
	defer driver.mutex.Unlock()
	jobName, buildNumber, err := getNames(req.Name)
	if err != nil {
		return volumeErrorResponse(fmt.Sprintf("Mount-%s: The volume name is invalid.", req.Name))
	}
	if err := newBuildCache(jobName, buildNumber, driver.cacheLocations).mount(); err != nil {
		return volumeErrorResponse(fmt.Sprintf("Mount-%s : Failed to mount overlay cache due to  %s", req.Name, err))
	}
	fmt.Println(fmt.Sprintf("Mount-%s: mounted cache", req.Name))
	return driver.Path(req)
}

func (driver cacheDriver) Unmount(req volume.Request) volume.Response {
	driver.mutex.Lock()
	defer driver.mutex.Unlock()
	return removeVolume(driver, req)
}

func (driver cacheDriver) Remove(req volume.Request) volume.Response {
	driver.mutex.Lock()
	defer driver.mutex.Unlock()
	return removeVolume(driver, req)
}

func (driver cacheDriver) Path(req volume.Request) volume.Response {

	jobName, buildNumber, err := getNames(req.Name)
	if err != nil {
		return volume.Response{Err: fmt.Sprintf("The volume name %s is invalid.", req.Name)}
	}
	return volume.Response{Mountpoint: getMergedPath(jobName, buildNumber, driver.cacheLocations.cacheMergedRootDir)}
}

func (driver cacheDriver) volume(jobName, name string) *volume.Volume {
	return &volume.Volume{
		Name:       jobName + "-" + name,
		Mountpoint: getMergedPath(jobName, name, driver.cacheLocations.cacheMergedRootDir),
	}
}

func removeVolume(driver cacheDriver, req volume.Request) volume.Response {
	jobName, buildNumber, err := getNames(req.Name)
	buildCache := newBuildCache(jobName, buildNumber, driver.cacheLocations)

	if err := buildCache.destroy(); err != nil {
		return volumeErrorResponse(fmt.Sprintf("Unmount-%s: Failed to destory volume : %s", req.Name, err))
	}
	fmt.Println(fmt.Sprintf("Unmount-%s: unmounted cache", req.Name))

	err = buildCache.cleanUpVolume()
	if err != nil {
		return volumeErrorResponse(fmt.Sprintf("Remove-%s: Failed to destory volume : %s", req.Name, err))
	}
	fmt.Println(fmt.Sprintf("Unmount-%s: cache deleted", req.Name))
	return driver.Path(req)
}

func getNames(volumeName string) (string, string, error) {
	names := strings.Split(volumeName, "-")
	if len(names) > 1 {
		return names[0], names[1], nil
	}
	return "", "", errors.New(volumeName + " is not valid.")
}
func volumeErrorResponse(err string) volume.Response {
	fmt.Println(err)
	return volume.Response{Err: err}
}
