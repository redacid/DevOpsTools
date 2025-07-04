-include .env
export

JAVA_HOME := $(HOME)/.jdks/jbr-17.0.14
#SHELL := /bin/bash
APP_NAME := devopstools
PACKAGE_OUTPUT_DIR := ./composeApp/build/compose/binaries
#APP_NAME_MAC := devopstools

PRJ_REPO := git@github.com:redacid/DevOpsTools.git
PRJ_REPO_HTTP := https://github.com/redacid/DevOpsTools.git
RELEASE_VERSION ?= 0.0.0
CLOBBER := --clobber
CURRENT_DATE := $(shell date '+%Y_%m_%d_%H_%M_%S')

# colors
GREEN = $(shell tput -Txterm setaf 2)
YELLOW = $(shell tput -Txterm setaf 3)
WHITE = $(shell tput -Txterm setaf 7)
RESET = $(shell tput -Txterm sgr0)
GRAY = $(shell tput -Txterm setaf 6)
TARGET_MAX_CHAR_NUM = 30

.EXPORT_ALL_VARIABLES:

all: help

copy_my_tasks:
	cp ~/.devopstools/tasks.json ./

backup_settings:
	cp ~/.devopstools/settings.json ~/.devopstools/settings_$(CURRENT_DATE).json


## Build and Publish
git-publish:
	make clean-workspace
	make build
	make package
	make git-release
	make git-upload-release
	#make clean-workspace

.ONESHELL:
clean-workspace:
	./gradlew clean

package: package-deb package-rpm

build:
	./gradlew build

package-deb:
	./gradlew packageReleaseDeb

package-rpm:
	./gradlew packageReleaseRpm

package-dmg:
	./gradlew packageReleaseDmg

package-msi:
	./gradlew.bat packageReleaseMsi

install-deb: package-deb
	sudo apt purge kubemanager -y
	sudo dpkg -i "$(PACKAGE_OUTPUT_DIR)/main-release/deb/"$(APP_NAME)"_"$(RELEASE_VERSION)"-1_amd64.deb"

git-release:
	gh release delete $(RELEASE_VERSION) --cleanup-tag -y --repo $(PRJ_REPO) 2>/dev/null;
	git tag -d $(RELEASE_VERSION) 2>/dev/null;
	gh release create $(RELEASE_VERSION) --generate-notes --notes "$(RELEASE_VERSION)" --repo $(PRJ_REPO)

git-upload-release: git-upload-deb-release git-upload-rpm-release

.ONESHELL:
git-upload-deb-release: package-deb
	gh release upload $(RELEASE_VERSION) "$(PACKAGE_OUTPUT_DIR)/main-release/deb/"$(APP_NAME)"_"$(RELEASE_VERSION)"-1_amd64.deb" --repo $(PRJ_REPO) $(CLOBBER)

.ONESHELL:
git-upload-rpm-release: package-rpm
	gh release upload $(RELEASE_VERSION) "$(PACKAGE_OUTPUT_DIR)/main-release/rpm/"$(APP_NAME)"-"$(RELEASE_VERSION)"-1.x86_64.rpm" --repo $(PRJ_REPO) $(CLOBBER)

.ONESHELL:
git-upload-mac-release: package-dmg
	gh release upload $(RELEASE_VERSION) $(PACKAGE_OUTPUT_DIR)/main-release/dmg/$(APP_NAME)-$(RELEASE_VERSION).dmg --repo $(PRJ_REPO_HTTP) $(CLOBBER)

.ONESHELL:
git-upload-win-release: package-msi
	gh release upload $(RELEASE_VERSION) $(PACKAGE_OUTPUT_DIR)/main-release/msi/$(APP_NAME)-$(RELEASE_VERSION).msi --repo $(PRJ_REPO) $(CLOBBER)

mac-install-req:
	brew install gh

.PHONY: build git-publish git-upload-release git-release clean-workspace all help

## Shows help. | Help
help:
	@echo ''
	@echo 'Usage:'
	@echo ''
	@echo '  ${YELLOW}make${RESET} ${GREEN}<target>${RESET}'
	@echo ''
	@echo 'Targets:'
	@awk '/^[a-zA-Z0-9\-_]+:/ { \
		helpMessage = match(lastLine, /^## (.*)/); \
		if (helpMessage) { \
		    if (index(lastLine, "|") != 0) { \
				stage = substr(lastLine, index(lastLine, "|") + 1); \
				printf "\n ${GRAY}%s: \n\n", stage;  \
			} \
			helpCommand = substr($$1, 0, index($$1, ":")-1); \
			helpMessage = substr(lastLine, RSTART + 3, RLENGTH); \
			if (index(lastLine, "|") != 0) { \
				helpMessage = substr(helpMessage, 0, index(helpMessage, "|")-1); \
			} \
			printf "  ${YELLOW}%-$(TARGET_MAX_CHAR_NUM)s${RESET} ${GREEN}%s${RESET}\n", helpCommand, helpMessage; \
		} \
	} \
	{ lastLine = $$0 }' $(MAKEFILE_LIST)
	@echo ''