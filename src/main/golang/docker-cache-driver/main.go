package main

import (
	"github.com/docker/go-plugins-helpers/volume"
)

func main() {
	driver := newCacheDriverDriver()

	handler := volume.NewHandler(driver)
	err := handler.ServeUnix("root", driver.name)
	if err != nil {
		panic(err.Error())
	}
}
