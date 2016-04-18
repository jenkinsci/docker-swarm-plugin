package main

import (
	"fmt"
	"os"
	"syscall"
)

type buildCache struct {
	mergeDir, upperDir, workDir string
	job, build                  string
}

func newBuildCache(job, build string) (*buildCache, error) {
	mergeDir := getMergedPath(job, build)
	upperDir := getUpperPath(job, build)
	workDir := getWorkDirPath(job, build)
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

func (buildCache *buildCache) destroy() error {
	emptyUpper, err := isEmpty(buildCache.upperDir)
	if err != nil {
		return err
	}
	if !emptyUpper {
		go func() {
			fmt.Println("Clone begin", buildCache.mergeDir)
			if err = cloneDir(buildCache.mergeDir, getBasePath(buildCache.job, buildCache.build)); err == nil {
				cacheState, _ := newCacheState()
				cacheState.State[buildCache.job] = buildCache.build
				cacheState.save()
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

func getMergedPath(jobName, name string) string {
	return fmt.Sprintf("%s/%s/merged/%s", cacheRootDir, jobName, name)
}
func getUpperPath(jobName, buildNumber string) string {
	return "/cache/" + jobName + "/upper/" + buildNumber
}
func getWorkDirPath(jobName, buildNumber string) string {
	return "/cache/" + jobName + "/work/" + buildNumber
}
