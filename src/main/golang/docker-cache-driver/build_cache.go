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
	err := os.MkdirAll(buildCache.mergeDir, 0755)
	if err != nil {
		return err
	}
	err = os.MkdirAll(buildCache.upperDir, 0755)
	if err != nil {
		return err
	}
	return os.MkdirAll(buildCache.workDir, 0755)
}

func (buildCache *buildCache) mount(baseBuildDir string) error {
	overlayDirs := fmt.Sprintf("lowerdir=%s,upperdir=%s,workdir=%s", baseBuildDir, buildCache.upperDir, buildCache.workDir)
	return syscall.Mount("overlay", buildCache.mergeDir, "overlay", 0, overlayDirs)
}

func (buildCache *buildCache) destroy(driver cacheDriver) error {
	volumeName := buildCache.job + "-" + buildCache.build
	emptyUpper, err := isEmpty(buildCache.upperDir)
	if err != nil {
		return err
	}
	if !emptyUpper {
		fmt.Println(fmt.Sprintf("Unmount-%s: Clone Begin, %s", volumeName, buildCache.mergeDir))
		if err = cloneDir(buildCache.mergeDir, getBasePath(buildCache.job, buildCache.build, driver.cacheLocations.cacheLowerRootDir)); err != nil {
			fmt.Println(fmt.Sprintf("Unmount-%s: Clone Dir failed %s", volumeName, err))
			return err
		} else {
			cacheState, _ := getCacheState(driver.cacheLocations.cacheLowerRootDir)
			cacheState.State[buildCache.job] = buildCache.build
			cacheState.save(driver.cacheLocations.cacheLowerRootDir)
			fmt.Println(fmt.Sprintf("Unmount-%s: Clone complete. Cloned to %s", volumeName, driver.cacheLocations.cacheLowerRootDir))
			return unMountVolume(buildCache)
		}
	} else {
		fmt.Println(fmt.Sprintf("Unmount-%s: Upper empty. cleaning up cache dirs", volumeName))
		return unMountVolume(buildCache)
	}
}
func unMountVolume(buildCache *buildCache) error {
	volumeName := buildCache.job + "-" + buildCache.build
	if err := syscall.Unmount(buildCache.mergeDir, 0); err != nil {
		fmt.Println(fmt.Sprintf("Unmount-%s: Syscall unmount %s failed. %s", volumeName, buildCache.mergeDir, err))
		return err
	}
	return nil
}
func (buildCache *buildCache) cleanUpVolume() error {
	volumeName := buildCache.job + "-" + buildCache.build

	if err := os.RemoveAll(buildCache.mergeDir); err != nil {
		fmt.Println(fmt.Sprintf("Remove-%s: Could not delete dir %s. %s", volumeName, buildCache.mergeDir, err))
		return err
	}
	if err := os.RemoveAll(buildCache.upperDir); err != nil {
		fmt.Println(fmt.Sprintf("Remove-%s: Could not delete dir %s. %s", volumeName, buildCache.upperDir, err))
		return err
	}
	if err := os.RemoveAll(buildCache.workDir); err != nil {
		fmt.Println(fmt.Sprintf("Remove-%s: Could not delete dir %s. %s", volumeName, buildCache.workDir, err))
		return err
	}
	return nil
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
