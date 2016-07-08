USER=		plandes
PROJ=		clj-actioncli
TARG=		target
DOC_DIR=	$(TARG)/doc
POM=		pom.xml

all:		package

.PHONEY:
package:	$(TARG)

.PHONEY:
deploy:		clean
	lein deploy clojars

.PHONEY:
docs:		$(DOC_DIR)

# https://github.com/weavejester/codox/wiki/Deploying-to-GitHub-Pages
$(DOC_DIR):
	rm -rf $(DOC_DIR) && mkdir -p $(DOC_DIR)
	git clone https://github.com/$(USER)/$(PROJ).git $(DOC_DIR)
	git update-ref -d refs/heads/gh-pages 
	git push --mirror
	( cd $(DOC_DIR) ; \
	  git symbolic-ref HEAD refs/heads/gh-pages ; \
	  rm .git/index ; \
	  git clean -fdx )
	lein codox

.PHONEY:
pushdocs:	$(DOC_DIR)
	( cd $(DOC_DIR) ; \
	  git add . ; \
	  git commit -am "new doc push" ; \
	  git push -u origin gh-pages )

.PHONEY:
install:
	lein install

$(TARG):
	lein jar

clean:
	rm -fr dev-resources $(TARG) $(POM)*
	rmdir test 2>/dev/null || true
	rmdir resources 2>/dev/null || true
