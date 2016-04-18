build:
	docker run --workdir="/go/src/app" -v $(shell pwd):/go/src/app  -e "GO15VENDOREXPERIMENT=1" \
	-e "GOOS=linux"   -e "GOARCH=amd64" golang:1.5.3 sh -xc "  go build  -o cache-driver "
install:
	docker run --workdir="/go/src/app" -v $(shell pwd):/go/src/app  -e "GO15VENDOREXPERIMENT=1" \
	-e "GOOS=linux"   -e "GOARCH=amd64" golang:1.5.3 sh -xc " go get github.com/Masterminds/glide && glide install && go build  -o cache-driver "
