## makefile automates the build and deployment for lein projects

PROJ_TYPE=		clojure

# make build dependencies
_ :=	$(shell [ ! -d .git ] && git init ; [ ! -d zenbuild ] && \
	  git submodule add https://github.com/plandes/zenbuild && make gitinit )

include ./zenbuild/main.mk

.PHONY: test
test:	clean
	lein test
