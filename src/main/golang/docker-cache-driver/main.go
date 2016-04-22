package main

import (
	"github.com/docker/go-plugins-helpers/volume"
	"io/ioutil"
	"os"
	"strconv"
	"syscall"
)

func main() {
	WithLock("/var/run/cache-driver.pid", func() {
		driver := newCacheDriverDriver()

		handler := volume.NewHandler(driver)
		err := handler.ServeUnix("root", driver.name)
		if err != nil {
			os.Exit(1)
		}
	})
}
func WithLock(pidFileName string, f func()) {
	pidFile, err := os.Create(pidFileName)
	if err != nil {
		os.Exit(1)
	}
	defer pidFile.Close()
	err = ioutil.WriteFile(pidFileName, []byte(strconv.Itoa(os.Getpid())), 0755)
	if err != nil {
		os.Exit(1)
	}

	err = syscall.Flock(int(pidFile.Fd()), syscall.LOCK_EX)
	if err != nil {
		os.Exit(1)
	}
	defer syscall.Flock(int(pidFile.Fd()), syscall.LOCK_UN)
	f()

}
