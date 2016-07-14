package main

import (
	"errors"
	"fmt"
	"github.com/docker/go-plugins-helpers/volume"
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
	fmt.Println("Starting... ")
	driver := cacheDriver{
		mutex:          &sync.Mutex{},
		name:           "cache-driver",
		cacheLocations: cacheLocations,
	}
	_, _ = newCacheState(driver) //handle error here
	return driver
}

func (driver cacheDriver) Get(req volume.Request) volume.Response {
	fmt.Println("Get Called... ")

	jobName, buildNumber, err := getNames(req.Name)
	buildCache, _ := newBuildCache(jobName, buildNumber, driver.cacheLocations)
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
	fmt.Println("Create Called :", req.Name)
	driver.mutex.Lock()
	defer driver.mutex.Unlock()
	jobName, buildNumber, err := getNames(req.Name)
	if err != nil {
		invalidVolumeNameErr := fmt.Sprintf("The volume name %s is invalid.", req.Name)
		fmt.Println(invalidVolumeNameErr)
		return volume.Response{Err: invalidVolumeNameErr}
	}
	cacheLocations := driver.cacheLocations
	buildCache, _ := newBuildCache(jobName, buildNumber, cacheLocations)
	fmt.Println("checking if exists", getMergedPath(jobName, buildNumber, cacheLocations.cacheMergedRootDir))
	if buildCache.exists() {
		return volume.Response{Err: fmt.Sprintf("The volume %s already exists", req.Name)}
	}

	buildCache.initDirs()
	cacheState, err := newCacheState(driver)
	baseBuildDir, err := cacheState.baseBuildDir(jobName, cacheLocations.cacheLowerRootDir)
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
	buildCache, _ := newBuildCache(jobName, buildNumber, driver.cacheLocations)
	err = buildCache.destroy(driver)
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
	return volume.Response{Mountpoint: getMergedPath(jobName, buildNumber, driver.cacheLocations.cacheMergedRootDir)}
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
		Mountpoint: getMergedPath(jobName, name, driver.cacheLocations.cacheMergedRootDir),
	}
}

func getNames(volumeName string) (string, string, error) {
	names := strings.Split(volumeName, "-")
	if len(names) > 1 {
		return names[0], names[1], nil
	}
	return "", "", errors.New(volumeName + " is not valid.")
}
