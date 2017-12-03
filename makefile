## makefile automates the build and deployment for lein projects

PROJ_TYPE=		clojure

include $(if $(ZBHOME),$(ZBHOME),../zenbuild)/main.mk

.PHONY: test
test:	clean
	lein test
