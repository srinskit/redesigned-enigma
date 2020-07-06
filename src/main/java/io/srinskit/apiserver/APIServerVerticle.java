package io.srinskit.apiserver;

import io.srinskit.adder.AdderService;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.core.http.HttpServerResponse;

public class APIServerVerticle extends AbstractVerticle {

	@Override
	public void start() {
		System.out.println("Starting an API server");
		Router router = Router.router(vertx);

		router.route("/add/:x/:y/").handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			AdderService adderService = AdderService.createProxy(vertx, "adder-service-address");
			Integer x = new Integer(routingContext.request().getParam("x"));
			Integer y = new Integer(routingContext.request().getParam("y"));
			adderService.operate(x, y, res -> {
				if (res.succeeded()) {
					response.end(res.result().toString());
				} else {
					response.end("ERROR" + res.cause());
				}
			});
		});

		vertx.createHttpServer().requestHandler(router).listen(8080);
	}

	@Override
	public void stop() {
		System.out.println("Stopping an API server");
	}
}
