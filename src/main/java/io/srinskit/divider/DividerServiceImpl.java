package io.srinskit.divider;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import io.micrometer.core.instrument.*;
import io.vertx.micrometer.backends.BackendRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DividerServiceImpl implements DividerService {
	private static final Logger LOGGER = LogManager.getLogger(DividerServiceImpl.class);
	public DividerServiceImpl(Vertx vertx) {
	}

	@Override
	public void operate(Integer a, Integer b, Handler<AsyncResult<Float>> resultHandler) {
		LOGGER.debug("Called me");
		MeterRegistry registry = BackendRegistries.getDefaultNow();
		Counter counter = registry.counter("Service.requestCount", "service", "divider");
		counter.increment(1);
		resultHandler.handle(Future.succeededFuture((float) a / (float) b));
	}
}