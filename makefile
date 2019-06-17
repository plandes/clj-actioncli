## makefile automates the build and deployment for lein projects

PROJ_TYPE=		clojure

include ./zenbuild/main.mk

.PHONY: test
test:	clean
	lein test
