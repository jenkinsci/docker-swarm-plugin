package main

import (
	"fmt"
	"os/exec"
)

func isEmpty(name string) (bool, error) {
	cmd := fmt.Sprintf("find %s -type f   ! \\( -name \\*.lock -o -name \\*.properties -o -name \\*.xml\\*  \\) | egrep '.*'", name)
	_, err := exec.Command(cmd).Output()
	return err != nil, nil
}

func cloneDir(source string, dest string) (err error) {
	return CopyDir(source, dest)

}
