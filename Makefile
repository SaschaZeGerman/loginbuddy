## this secret is used for testing here: net.loginbuddy.config/src/test/java/net/loginbuddy/config/loginbuddy/TestLoginbuddyConfig.java
export SECRET_OBFUSCATION = mtL4BNYmjhy1GltxLOsk4MoRnzeIQ8YK

define BUILD_DOCKER
	docker build --no-cache --tag saschazegerman/loginbuddy-base:latest -f Dockerfile_base .
	docker build --no-cache --tag saschazegerman/loginbuddy:latest .
	docker build --no-cache --tag saschazegerman/loginbuddy-oidcdr:latest -f Dockerfile_oidcdr .
endef

# Compile the code and build the docker images
#
build_all:
	mvn clean install
	$(BUILD_DOCKER)

# Compile the code and build docker images using the builder image
# Use this target if you do not have Maven installed
# Run the target 'build_builder' before running this target
#
build_all_non_dev:
	docker run -v `pwd`:/tmp saschazegerman/loginbuddy-builder:latest mvn -f "/tmp/pom.xml" clean package
	$(BUILD_DOCKER)

# Creates a docker image that contains Java and Maven and compiles the code
# This is useful if you do not want to fiddle around with Java versions and Maven
# Run this target before running 'build_all_non_dev'
#
build_builder:
	docker build --no-cache --tag saschazegerman/loginbuddy-builder:latest -f Dockerfile_builder .

# Compile the code and build the docker images for amd64 architectures
#
define BUILD_DOCKER_AMD64
	docker buildx build --platform linux/amd64 --no-cache --tag saschazegerman/loginbuddy-base-amd64:latest -f Dockerfile_base_amd64 .
	docker buildx build --platform linux/amd64 --no-cache --tag saschazegerman/loginbuddy-amd64:latest -f Dockerfile_amd64 .
	docker buildx build --platform linux/amd64 --no-cache --tag saschazegerman/loginbuddy-oidcdr-amd64:latest -f Dockerfile_oidcdr_amd64 .
endef

build_all_amd64:
	mvn clean install
	$(BUILD_DOCKER_AMD64)

build_all_non_dev_amd64:
	docker run -v `pwd`:/tmp saschazegerman/loginbuddy-builder-amd64:latest mvn -f "/tmp/pom.xml" clean package
	$(BUILD_DOCKER_AMD64)

build_builder_amd64:
	docker buildx build --platform linux/amd64 --no-cache --tag saschazegerman/loginbuddy-builder-amd64:latest -f Dockerfile_builder_amd64 .