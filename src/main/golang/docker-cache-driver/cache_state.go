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
	_, err := os.Stat(stateFile)
	if err != nil {
		volumes := make(map[string]string)
		data := cacheState{
			State: volumes,
		}
		fileData, err := json.Marshal(data)
		if err != nil {
			return &cacheState{}, err
		}
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

func (cacheState *cacheState) updateState(cacheLowerRootDir, newLatest string) error {
	cacheState.State["latest"] = newLatest
	return cacheState.save(cacheLowerRootDir)
}

func (cacheState *cacheState) getBaseBuild(cacheLowerRootDir string) (string, error) {
	if baseBuild, ok := cacheState.State["latest"]; ok {
		return baseBuild, nil
	} else {
		baseBuild := "0"
		cacheState.State["latest"] = "0"
		cacheState.save(cacheLowerRootDir)
		if err := os.MkdirAll(path.Join(cacheLowerRootDir, baseBuild), 0755); err != nil {
			return "", err
		}
		return baseBuild, nil
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
