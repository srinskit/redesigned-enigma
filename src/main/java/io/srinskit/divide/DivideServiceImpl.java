package io.srinskit.divide;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import io.vertx.micrometer.*;
import io.micrometer.core.instrument.*;
import io.vertx.micrometer.backends.BackendRegistries;

public class DivideServiceImpl implements DivideService {
	public DivideServiceImpl(Vertx vertx) {
	}

	@Override
	public void operate(Integer a, Integer b, Handler<AsyncResult<Float>> resultHandler) {
		System.out.println("Called me");
		MeterRegistry registry = BackendRegistries.getDefaultNow();
		Counter counter = registry.counter("Service.requestCount", "service", "divide");
		counter.increment(1);
		resultHandler.handle(Future.succeededFuture((float) a / (float) b));
	}
}