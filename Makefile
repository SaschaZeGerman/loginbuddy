build_all:
	mvn clean install
	docker build --no-cache --tag saschazegerman/loginbuddy:latest .
	docker build --no-cache --tag saschazegerman/loginbuddy:latest-demo -f Dockerfile_demosetup .
	docker build --no-cache --tag saschazegerman/loginbuddy:latest-sidecar -f Dockerfile_sidecar .
	docker build --no-cache --tag saschazegerman/loginbuddy:latest-oidcdr -f Dockerfile_oidcdr .
	docker build --no-cache --tag saschazegerman/loginbuddy:latest-test -f Dockerfile_test .

docker_run_demo:
	docker-compose -f docker-compose-demosetup.yml up -d

docker_stop_demo:
	docker-compose -f docker-compose-demosetup.yml down

initialize_dev:
	sh initialize-dev-environment.sh