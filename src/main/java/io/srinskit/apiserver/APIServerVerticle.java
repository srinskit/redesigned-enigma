package io.srinskit.apiserver;

import io.srinskit.adder.AdderService;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.core.http.HttpServerResponse;
import java.io.*;
import io.vertx.micrometer.*;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.config.MeterFilter;
import io.vertx.micrometer.backends.BackendRegistries;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class APIServerVerticle extends AbstractVerticle {
	static String historyFileName = "data/api-server-history.txt";

	@Override
	public void start() {
		System.out.println("Starting an API server");
		Router router = Router.router(vertx);
		MeterRegistry registry = BackendRegistries.getDefaultNow();
		// Counter counter=registry.counter("api_server_http_requests_count",  "api", "/add/");
		// Timer timer=registry.timer("api_server_http_responseTime","api", "/add/");
		
		Pattern pattern = Pattern.compile("/add/.*/.*");
		registry.config().meterFilter(
  		MeterFilter.replaceTagValues(Label.HTTP_PATH.toString(), actualPath -> {
    	Matcher m = pattern.matcher(actualPath);
   		if (m.matches()) {
      		return "/add/:x/:y/";
    	}
    		return actualPath;
  		}, ""));
		router.route("/add/:x/:y/").handler(routingContext -> {
			// counter.increment(1);
			// timer.record( () ->{
			HttpServerResponse response = routingContext.response();
			AdderService adderService = AdderService.createProxy(vertx, "adder-service-address");
			Integer x = new Integer(routingContext.request().getParam("x"));
			Integer y = new Integer(routingContext.request().getParam("y"));
			adderService.operate(x, y, res -> {
				String reply = "";
				if (res.succeeded()) {
					reply = String.format("%d + %d = %d\n", x, y, res.result());
				} else {
					reply = String.format("%d + %d = %s\n", x, y, "ERROR, " + res.cause());
				}
				try {
					FileWriter fileWriter = new FileWriter(historyFileName, true);
					fileWriter.write(reply);
					fileWriter.close();
				} catch (IOException ex) {
					System.out.println("Error writing to history file");
					reply = "ERROR: " + ex.getMessage();
				}
				response.end(reply);
			});
			
		// });
	});
		router.route("/history").handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			response.sendFile(historyFileName);
			response.end();
		});

		router.route("/clear_history").handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			String reply = "Done";
			try {
				FileWriter fileWriter = new FileWriter(historyFileName);
				fileWriter.close();
			} catch (IOException ex) {
				System.out.println("Error writing to history file");
				reply = "ERROR: " + ex.getMessage();
			}
			response.end(reply);
		});

		vertx.createHttpServer().requestHandler(router).listen(8080);
	}

	@Override
	public void stop() {
		System.out.println("Stopping an API server");
	}
}
