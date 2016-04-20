package main

import (
	"errors"
	"os"
	"path/filepath"
	"regexp"
)

func isEmpty(dirPath string) (bool, error) {
	//*.properties *.xml
	err := filepath.Walk(dirPath, func(path string, f os.FileInfo, _ error) error {
		if !f.IsDir() {
			r, err := regexp.MatchString("(.properties|.xml)$", f.Name())
			if err == nil && !r {
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
