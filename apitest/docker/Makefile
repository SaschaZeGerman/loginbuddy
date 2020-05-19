run-test: prepare-test
	docker-compose -f docker-compose-test.yml up

stop-test:
	docker-compose -f docker-compose-test.yml down

prepare-test:
	cp -r ../../net.loginbuddy.test/target/classes/* test-classes
	cp testCustomLoginbuddyConfig.json.bak testCustomLoginbuddyConfig.json