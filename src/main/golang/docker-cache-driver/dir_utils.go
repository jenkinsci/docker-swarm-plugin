package main

import (
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
)

func isEmpty(dirPath string) (bool, error) {
	//*.properties *.xml
	err := filepath.Walk(dirPath, func(path string, f os.FileInfo, _ error) error {
		if !f.IsDir() {
			r, err := regexp.MatchString("(.properties|.xml|.credentials|.repositories|.sha1|.pom|.pom.lastUpdated|.lock)$", f.Name())
			if err == nil && !r {
				fmt.Printf("Found new file %s %s\n", path, f.Name())
				return errors.New("")
			}
		}
		return nil
	})
	return err == nil, nil
}

func cloneDir(source string, dest string) (err error) {
	return CopyDir(source, dest)
}

func mkdirs(dirs ...string) error {
	for _, dir := range dirs {
		if err := os.MkdirAll(dir, 0755); err != nil {
			return err
		}
	}
	return nil
}
