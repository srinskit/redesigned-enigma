package io.srinskit.divider;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@ProxyGen
public interface DividerService {
	static DividerService create(Vertx vertx) {
		return new DividerServiceImpl(vertx);
	}

	static DividerService createProxy(Vertx vertx, String address) {
		return new DividerServiceVertxEBProxy(vertx, address);
	}

	void operate(Integer a, Integer b, Handler<AsyncResult<Float>> resultHandler);
}