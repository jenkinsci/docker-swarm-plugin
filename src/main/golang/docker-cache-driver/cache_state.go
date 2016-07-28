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
func getCacheState(fileLocationDir string) (*cacheState, error) {
	stateFile := getStateFile(fileLocationDir)
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

func newCacheState(fileLocationDir string) (*cacheState, error) {
	stateFile := getStateFile(fileLocationDir)
	_, err := os.Stat(stateFile)
	if err != nil {
		volumes := make(map[string]string)
		data := cacheState{
			State: volumes,
		}
		fileData, _ := json.Marshal(data)
		return &data, ioutil.WriteFile(stateFile, fileData, 0600)
	}
	return &cacheState{}, nil
}
func (cacheState *cacheState) baseBuildDir(jobName, cacheLowerRootDir string) (string, error) {
	if baseBuild, ok := cacheState.State[jobName]; ok {
		return getBasePath(jobName, baseBuild, cacheLowerRootDir), nil
	} else {
		baseBuild := "0"
		cacheState.State[jobName] = "0"
		cacheState.save(cacheLowerRootDir)
		baseBuildCachePath := getBasePath(jobName, baseBuild, cacheLowerRootDir)
		if err := os.MkdirAll(baseBuildCachePath, 0755); err != nil {
			return "", err
		}
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
