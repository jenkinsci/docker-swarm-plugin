package main

import (
	"fmt"
	"os"
	"path"
	"syscall"
)

type buildVolume struct {
	job, build string
	rootDirs   rootDirs
}

func newBuildVolume(job, build string, cacheRootDirs *rootDirs) *buildVolume {
	return &buildVolume{
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

func (buildVolume *buildVolume) init() error {
	return buildVolume.rootDirs.mkdirs()
}

func (volume *buildVolume) mount() error {
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

func (buildVolume *buildVolume) destroy() error {
	volumeName := buildVolume.job + "-" + buildVolume.build
	emptyUpper, err := isEmpty(buildVolume.rootDirs.upper)
	if err != nil {
		return err
	}
	if !emptyUpper {
		newLowerDir := path.Join(buildVolume.rootDirs.lower, buildVolume.build)
		fmt.Println(fmt.Sprintf("Unmount-%s: Cloning %s to %s", volumeName, buildVolume.rootDirs.merged, newLowerDir))
		if err = cloneDir(buildVolume.rootDirs.merged, newLowerDir); err != nil {
			fmt.Println(fmt.Sprintf("Unmount-%s: Clone Dir failed %s", volumeName, err))
			return err
		}
		cacheState, _ := getCacheState(buildVolume.rootDirs.lower)
		cacheState.updateState(buildVolume.rootDirs.lower, buildVolume.build)

		fmt.Println(fmt.Sprintf("Unmount-%s: Clone complete. Cloned to %s", volumeName, newLowerDir))
		return unMountVolume(buildVolume)
	} else {
		fmt.Println(fmt.Sprintf("Unmount-%s: Upper empty. cleaning up cache dirs", volumeName))
		return unMountVolume(buildVolume)
	}
}

func unMountVolume(buildVolume *buildVolume) error {
	volumeName := buildVolume.job + "-" + buildVolume.build
	if err := syscall.Unmount(buildVolume.rootDirs.merged, 0); err != nil {
		fmt.Println(fmt.Sprintf("Unmount-%s: Syscall unmount %s failed. %s", volumeName, buildVolume.rootDirs.merged, err))
		return err
	}
	return nil
}

func (buildVolume *buildVolume) cleanUpVolume() error {
	volumeName := buildVolume.job + "-" + buildVolume.build

	if err := os.RemoveAll(buildVolume.rootDirs.merged); err != nil {
		fmt.Println(fmt.Sprintf("Unmount-%s: Could not delete dir %s. %s", volumeName, buildVolume.rootDirs.merged, err))
		return err
	}
	if err := os.RemoveAll(buildVolume.rootDirs.upper); err != nil {
		fmt.Println(fmt.Sprintf("Unmount-%s: Could not delete dir %s. %s", volumeName, buildVolume.rootDirs.upper, err))
		return err
	}
	if err := os.RemoveAll(buildVolume.rootDirs.work); err != nil {
		fmt.Println(fmt.Sprintf("Unmount-%s: Could not delete dir %s. %s", volumeName, buildVolume.rootDirs.work, err))
		return err
	}
	return nil
}

func (buildVolume *buildVolume) exists() bool {
	_, err := os.Stat(buildVolume.rootDirs.work)
	return err == nil
}
