package main

import (
	"fmt"
	"os"
	"path"
	"syscall"
)

type buildCache struct {
	mergeDir, upperDir, workDir string
	jobLowerRootDir             string
	job, build                  string
}

func newBuildCache(job, build string, rootDirs *rootDirs) *buildCache {
	mergeDir := getMergedPath(job, build, rootDirs.merged)
	upperDir := getUpperPath(job, build, rootDirs.upper)
	workDir := getWorkDirPath(job, build, rootDirs.work)
	jobLowerRootDir := getJobLowerRootDir(rootDirs.lower, job)
	return &buildCache{job: job, build: build, mergeDir: mergeDir, upperDir: upperDir, workDir: workDir, jobLowerRootDir: jobLowerRootDir}

}

func (buildCache *buildCache) init() error {
	if _, err := newCacheState(buildCache.jobLowerRootDir); err != nil {
		return err
	}
	return mkdirs(buildCache.mergeDir, buildCache.upperDir, buildCache.workDir)
}

func (buildCache *buildCache) mount() error {
	cacheState, err := getCacheState(buildCache.jobLowerRootDir)
	if err != nil {
		return err
	}
	baseBuild, err := cacheState.getBaseBuild(buildCache.jobLowerRootDir)
	if err != nil {
		return err
	}
	baseBuildDir := path.Join(buildCache.jobLowerRootDir, baseBuild)
	overlayDirs := fmt.Sprintf("lowerdir=%s,upperdir=%s,workdir=%s", baseBuildDir, buildCache.upperDir, buildCache.workDir)
	return syscall.Mount("overlay", buildCache.mergeDir, "overlay", 0, overlayDirs)
}

func (buildCache *buildCache) destroy() error {
	volumeName := buildCache.job + "-" + buildCache.build
	emptyUpper, err := isEmpty(buildCache.upperDir)
	if err != nil {
		return err
	}
	if !emptyUpper {
		newLowerDir := path.Join(buildCache.jobLowerRootDir, buildCache.build)
		fmt.Println(fmt.Sprintf("Unmount-%s: Cloning %s to %s", volumeName, buildCache.mergeDir, newLowerDir))
		if err = cloneDir(buildCache.mergeDir, newLowerDir); err != nil {
			fmt.Println(fmt.Sprintf("Unmount-%s: Clone Dir failed %s", volumeName, err))
			return err
		}
		cacheState, _ := getCacheState(buildCache.jobLowerRootDir)
		cacheState.updateState(buildCache.jobLowerRootDir, buildCache.build)

		fmt.Println(fmt.Sprintf("Unmount-%s: Clone complete. Cloned to %s", volumeName, newLowerDir))
		return unMountVolume(buildCache)
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
		fmt.Println(fmt.Sprintf("Unmount-%s: Could not delete dir %s. %s", volumeName, buildCache.mergeDir, err))
		return err
	}
	if err := os.RemoveAll(buildCache.upperDir); err != nil {
		fmt.Println(fmt.Sprintf("Unmount-%s: Could not delete dir %s. %s", volumeName, buildCache.upperDir, err))
		return err
	}
	if err := os.RemoveAll(buildCache.workDir); err != nil {
		fmt.Println(fmt.Sprintf("Unmount-%s: Could not delete dir %s. %s", volumeName, buildCache.workDir, err))
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
func getJobLowerRootDir(cacheLowerRootDir, jobName string) string {
	return path.Join(cacheLowerRootDir, jobName)
}
