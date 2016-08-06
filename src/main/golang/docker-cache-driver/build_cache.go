package main

import (
	"fmt"
	"os"
	"path"
	"syscall"
)

type buildCache struct {
	job, build string
	rootDirs   rootDirs
}

func newBuildCache(job, build string, cacheRootDirs *rootDirs) *buildCache {
	return &buildCache{
		job:   job,
		build: build,
		rootDirs: rootDirs{
			merged: path.Join(cacheRootDirs.merged, job, build),
			upper:  path.Join(cacheRootDirs.upper, job, build),
			work:   path.Join(cacheRootDirs.work, job, build),
			lower:  path.Join(cacheRootDirs.lower, job), // We don't know the base build yet
		},
	}
}

func (buildCache *buildCache) init() error {
	if err := buildCache.rootDirs.mkdirs(); err != nil {
		return err
	}
	_, err := newCacheState(buildCache.rootDirs.lower)
	return err
}

func (volume *buildCache) mount() error {
	cacheState, err := getCacheState(volume.rootDirs.lower)
	if err != nil {
		return err
	}
	baseBuild, err := cacheState.getBaseBuild(volume.rootDirs.lower)
	if err != nil {
		return err
	}
	baseBuildDir := path.Join(volume.rootDirs.lower, baseBuild)
	overlayDirs := fmt.Sprintf("lowerdir=%s,upperdir=%s,workdir=%s", baseBuildDir, volume.rootDirs.upper, volume.rootDirs.work)
	return syscall.Mount("overlay", volume.rootDirs.merged, "overlay", 0, overlayDirs)
}

func (buildCache *buildCache) destroy() error {
	volumeName := buildCache.job + "-" + buildCache.build
	emptyUpper, err := isEmpty(buildCache.rootDirs.upper)
	if err != nil {
		return err
	}
	if !emptyUpper {
		newLowerDir := path.Join(buildCache.rootDirs.lower, buildCache.build)
		fmt.Println(fmt.Sprintf("Unmount-%s: Cloning %s to %s", volumeName, buildCache.rootDirs.merged, newLowerDir))
		if err = cloneDir(buildCache.rootDirs.merged, newLowerDir); err != nil {
			fmt.Println(fmt.Sprintf("Unmount-%s: Clone Dir failed %s", volumeName, err))
			return err
		}
		cacheState, _ := getCacheState(buildCache.rootDirs.lower)
		cacheState.updateState(buildCache.rootDirs.lower, buildCache.build)

		fmt.Println(fmt.Sprintf("Unmount-%s: Clone complete. Cloned to %s", volumeName, newLowerDir))
		return unMountVolume(buildCache)
	} else {
		fmt.Println(fmt.Sprintf("Unmount-%s: Upper empty. cleaning up cache dirs", volumeName))
		return unMountVolume(buildCache)
	}
}

func unMountVolume(buildCache *buildCache) error {
	volumeName := buildCache.job + "-" + buildCache.build
	if err := syscall.Unmount(buildCache.rootDirs.merged, 0); err != nil {
		fmt.Println(fmt.Sprintf("Unmount-%s: Syscall unmount %s failed. %s", volumeName, buildCache.rootDirs.merged, err))
		return err
	}
	return nil
}

func (buildCache *buildCache) cleanUpVolume() error {
	volumeName := buildCache.job + "-" + buildCache.build

	if err := os.RemoveAll(buildCache.rootDirs.merged); err != nil {
		fmt.Println(fmt.Sprintf("Unmount-%s: Could not delete dir %s. %s", volumeName, buildCache.rootDirs.merged, err))
		return err
	}
	if err := os.RemoveAll(buildCache.rootDirs.upper); err != nil {
		fmt.Println(fmt.Sprintf("Unmount-%s: Could not delete dir %s. %s", volumeName, buildCache.rootDirs.upper, err))
		return err
	}
	if err := os.RemoveAll(buildCache.rootDirs.work); err != nil {
		fmt.Println(fmt.Sprintf("Unmount-%s: Could not delete dir %s. %s", volumeName, buildCache.rootDirs.work, err))
		return err
	}
	return nil
}

func (buildCache *buildCache) exists() bool {
	_, err := os.Stat(buildCache.rootDirs.work)
	return err == nil
}
