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
			r, err := regexp.MatchString("(.properties|.xml|.credentials)$", f.Name())
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
