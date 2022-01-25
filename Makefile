build_all:
	mvn clean install
	docker build --no-cache --tag saschazegerman/loginbuddy:latest .
	docker build --no-cache --tag saschazegerman/loginbuddy-demo:latest -f Dockerfile_demosetup .
	docker build --no-cache --tag saschazegerman/loginbuddy-sidecar:latest -f Dockerfile_sidecar .
	docker build --no-cache --tag saschazegerman/loginbuddy-oidcdr:latest -f Dockerfile_oidcdr .
	docker build --no-cache --tag saschazegerman/loginbuddy-test:latest -f Dockerfile_test .
	docker build --no-cache --tag saschazegerman/loginbuddy-demoserver:latest -f Dockerfile_demoserver .

docker_run_demo:
	docker-compose -f docker-compose-demosetup.yml up

docker_stop_demo:
	docker-compose -f docker-compose-demosetup.yml down

initialize_dev:
	sh initialize-dev-environment.sh