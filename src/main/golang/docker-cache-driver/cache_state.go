package main

import (
	"encoding/json"
	"io/ioutil"
	"os"
	"path"
)

type cacheState struct {
	State map[string]string `json:"state"`
}

func getStateFile(cacheLowerRootDir string) string {
	return path.Join(cacheLowerRootDir, "cache-state.json")
}

func newCacheState(driver cacheDriver) (*cacheState, error) {
	stateFile := getStateFile(driver.cacheLocations.cacheLowerRootDir)
	_, err := os.Stat(stateFile)
	if err != nil {
		volumes := make(map[string]string)
		data := cacheState{
			State: volumes,
		}
		fileData, _ := json.Marshal(data)
		return &data, ioutil.WriteFile(stateFile, fileData, 0600)
	} else {
		fileData, err := ioutil.ReadFile(stateFile)
		if err != nil {
			return &cacheState{}, err
		}
		var data cacheState
		e := json.Unmarshal(fileData, &data)
		if e != nil {
			return &cacheState{}, err
		}
		return &data, nil
	}
}
func (cacheState *cacheState) baseBuildDir(jobName, cacheLowerRootDir string) (string, error) {
	if baseBuild, ok := cacheState.State[jobName]; ok {
		return getBasePath(jobName, baseBuild, cacheLowerRootDir), nil
	} else {
		baseBuild := "0"
		cacheState.State[jobName] = "0"
		cacheState.save(cacheLowerRootDir)
		baseBuildCachePath := getBasePath(jobName, baseBuild, cacheLowerRootDir)
		os.MkdirAll(baseBuildCachePath, 0755)
		return baseBuildCachePath, nil
	}
}

func getBasePath(jobName, buildNumber, cacheLowerRootDir string) string {
	return path.Join(cacheLowerRootDir, jobName, buildNumber)
}

func (cacheState *cacheState) save(cacheLowerRootDir string) error {
	stateFile := getStateFile(cacheLowerRootDir)
	fileData, _ := json.Marshal(cacheState)
	return ioutil.WriteFile(stateFile, fileData, 0600)
}
