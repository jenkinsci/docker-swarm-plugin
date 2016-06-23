package main

import (
	"encoding/json"
	"io/ioutil"
	"os"
	"path"
)

var stateFile = path.Join(cacheLowerRootDir, "cache-state.json")

type cacheState struct {
	State map[string]string `json:"state"`
}

func newCacheState() (*cacheState, error) {
	os.MkdirAll(cacheLowerRootDir, 0755)
	os.MkdirAll(cacheUpperRootDir, 0755)
	os.MkdirAll(cacheWorkRootDir, 0755)
	os.MkdirAll(cacheMergedRootDir, 0755)
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
func (cacheState *cacheState) baseBuildDir(jobName string) (string, error) {
	if baseBuild, ok := cacheState.State[jobName]; ok {
		return getBasePath(jobName, baseBuild), nil
	} else {
		baseBuild := "0"
		cacheState.State[jobName] = "0"
		cacheState.save()
		baseBuildCachePath := getBasePath(jobName, baseBuild)
		os.MkdirAll(baseBuildCachePath, 0755)
		return baseBuildCachePath, nil
	}
}

func getBasePath(jobName, buildNumber string) string {
	return path.Join(cacheLowerRootDir, jobName, buildNumber)
}

func (cacheState *cacheState) save() error {
	fileData, _ := json.Marshal(cacheState)
	return ioutil.WriteFile(stateFile, fileData, 0600)
}
