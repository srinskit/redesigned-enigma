package io.srinskit.adder;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@ProxyGen
public interface AdderService {
	static AdderService create(Vertx vertx) {
		return new AdderServiceImpl(vertx);
	}

	static AdderService createProxy(Vertx vertx, String address) {
		return new AdderServiceVertxEBProxy(vertx, address);
	}

	void operate(Integer a, Integer b, Handler<AsyncResult<Integer>> resultHandler);
}