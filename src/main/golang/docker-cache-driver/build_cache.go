package main

import (
	"fmt"
	"os"
	"path"
	"syscall"
)

type buildCache struct {
	mergeDir, upperDir, workDir string
	job, build                  string
}

func newBuildCache(job, build string, cacheLocations *cacheLocations) (*buildCache, error) {
	mergeDir := getMergedPath(job, build, cacheLocations.cacheMergedRootDir)
	upperDir := getUpperPath(job, build, cacheLocations.cacheUpperRootDir)
	workDir := getWorkDirPath(job, build, cacheLocations.cacheWorkRootDir)
	return &buildCache{job: job, build: build, mergeDir: mergeDir, upperDir: upperDir, workDir: workDir}, nil

}

func (buildCache *buildCache) initDirs() error {
	os.MkdirAll(buildCache.mergeDir, 0755)
	os.MkdirAll(buildCache.upperDir, 0755)
	os.MkdirAll(buildCache.workDir, 0755)
	return nil

}

func (buildCache *buildCache) mount(baseBuildDir string) error {
	overlayDirs := fmt.Sprintf("lowerdir=%s,upperdir=%s,workdir=%s", baseBuildDir, buildCache.upperDir, buildCache.workDir)
	return syscall.Mount("overlay", buildCache.mergeDir, "overlay", 0, overlayDirs)
}

func (buildCache *buildCache) destroy(driver cacheDriver) error {
	emptyUpper, err := isEmpty(buildCache.upperDir)
	if err != nil {
		return err
	}
	if !emptyUpper {
		go func() {
			fmt.Println("Clone begin", buildCache.mergeDir)
			if err = cloneDir(buildCache.mergeDir, getBasePath(buildCache.job, buildCache.build, driver.cacheLocations.cacheLowerRootDir)); err == nil {
				cacheState, _ := newCacheState(driver)
				cacheState.State[buildCache.job] = buildCache.build
				cacheState.save(driver.cacheLocations.cacheLowerRootDir)
				fmt.Println("Clone end", buildCache.mergeDir)
				cleanUpVolume(buildCache)
			}
		}()
		return nil
	} else {
		return cleanUpVolume(buildCache)
	}
}
func cleanUpVolume(buildCache *buildCache) error {
	if err := syscall.Unmount(buildCache.mergeDir, 0); err != nil {
		return err
	}

	if err := os.RemoveAll(buildCache.mergeDir); err != nil {
		return err
	}
	if err := os.RemoveAll(buildCache.upperDir); err != nil {
		return err
	}
	return os.RemoveAll(buildCache.workDir)
}

func (buildCache *buildCache) exists() bool {
	_, err := os.Stat(buildCache.mergeDir)
	return err == nil
}

func getMergedPath(jobName, buildNumber, cacheMergedRootDir string) string {
	return path.Join(cacheMergedRootDir, jobName, buildNumber)
}
func getUpperPath(jobName, buildNumber, cacheUpperRootDir string) string {
	return path.Join(cacheUpperRootDir, jobName, buildNumber)
}
func getWorkDirPath(jobName, buildNumber, cacheWorkRootDir string) string {
	return path.Join(cacheWorkRootDir, jobName, buildNumber)
}
