
package io.srinskit.divide;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

public class DivideServiceVerticle extends AbstractVerticle {
	ServiceBinder binder;
	DivideServiceImpl service;
	MessageConsumer<JsonObject> consumer;

	@Override
	public void start() {
		binder = new ServiceBinder(vertx);
		service = new DivideServiceImpl(vertx);
		consumer = new ServiceBinder(vertx).setAddress("divide-service-address").register(DivideService.class, service);
	}

	@Override
	public void stop() {
		binder.unregister(consumer);
	}
}
