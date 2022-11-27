build_all:
	mvn clean install
	docker build --no-cache --tag saschazegerman/loginbuddy-base:latest -f Dockerfile_base .
	docker build --no-cache --tag saschazegerman/loginbuddy:latest .
	docker build --no-cache --tag saschazegerman/loginbuddy-sidecar:latest -f Dockerfile_sidecar .
	docker build --no-cache --tag saschazegerman/loginbuddy-oidcdr:latest -f Dockerfile_oidcdr .
