package io.srinskit.adder;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class AdderServiceImpl implements AdderService {
	public AdderServiceImpl(Vertx vertx) {
	}

	@Override
	public void operate(Integer a, Integer b, Handler<AsyncResult<Integer>> resultHandler) {
		System.out.println("Called me");
		resultHandler.handle(Future.succeededFuture(a + b));
	}
}