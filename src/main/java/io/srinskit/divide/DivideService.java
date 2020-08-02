package io.srinskit.divide;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@ProxyGen
public interface DivideService {
	static DivideService create(Vertx vertx) {
		return new DivideServiceImpl(vertx);
	}

	static DivideService createProxy(Vertx vertx, String address) {
		return new DivideServiceVertxEBProxy(vertx, address);
	}

	void operate(Integer a, Integer b, Handler<AsyncResult<Float>> resultHandler);
}