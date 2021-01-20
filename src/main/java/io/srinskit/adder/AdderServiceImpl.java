package io.srinskit.adder;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.vertx.micrometer.backends.BackendRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdderServiceImpl implements AdderService {
	private static final Logger LOGGER = LogManager.getLogger(AdderServiceImpl.class);
	public AdderServiceImpl(Vertx vertx) {
	}

	@Override
	public void operate(Integer a, Integer b, Handler<AsyncResult<Integer>> resultHandler) {
		LOGGER.debug("Called me");
		MeterRegistry registry = BackendRegistries.getDefaultNow();
		Counter counter = registry.counter("service.requestCount", "service", "adder");
		counter.increment(1);
		resultHandler.handle(Future.succeededFuture(a + b));
	}
}