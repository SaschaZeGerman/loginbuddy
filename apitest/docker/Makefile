run-test: prepare-test
	docker-compose -f docker-compose-test.yml up

stop-test:
	docker-compose -f docker-compose-test.yml down

run-test-hazelcast: prepare-test
	docker-compose -f docker-compose-test-hazelcast.yml up

stop-test-hazelcast:
	docker-compose -f docker-compose-test-hazelcast.yml down

run-test-flows: prepare-test
	docker-compose -f docker-compose-test-flows.yml up

stop-test-flows:
	docker-compose -f docker-compose-test-flows.yml down

prepare-test:
	cp -r ../../net.loginbuddy.test/target/classes/* test-classes
	cp testCustomLoginbuddyConfig.json.bak testCustomLoginbuddyConfig.json