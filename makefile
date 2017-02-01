## makefile automates the build and deployment for lein projects

# location of the http://github.com/plandes/clj-zenbuild cloned directory
ZBHOME ?=	../clj-zenbuild

all:		info

include $(ZBHOME)/src/mk/compile.mk

# checkdep creates clojure compiled files creating a file:/.../clojure failing
# the resource test
.PHONY: test
test:	clean
	lein test
