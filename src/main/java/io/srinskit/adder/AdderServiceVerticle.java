
package io.srinskit.adder;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

public class AdderServiceVerticle extends AbstractVerticle {
	ServiceBinder binder;
	AdderServiceImpl service;
	MessageConsumer<JsonObject> consumer;

	@Override
	public void start() {
		binder = new ServiceBinder(vertx);
		service = new AdderServiceImpl(vertx);
		consumer = new ServiceBinder(vertx).setAddress("adder-service-address").register(AdderService.class, service);
	}

	@Override
	public void stop() {
		binder.unregister(consumer);
		System.out.println("adder verticle stopped");
	}
}
