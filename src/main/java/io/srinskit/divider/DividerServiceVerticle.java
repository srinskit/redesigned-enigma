
package io.srinskit.divider;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

public class DividerServiceVerticle extends AbstractVerticle {
	ServiceBinder binder;
	DividerServiceImpl service;
	MessageConsumer<JsonObject> consumer;

	@Override
	public void start() {
		binder = new ServiceBinder(vertx);
		service = new DividerServiceImpl(vertx);
		consumer = new ServiceBinder(vertx).setAddress("divider-service-address").register(DividerService.class, service);
	}

	@Override
	public void stop() {
		binder.unregister(consumer);
	}
}
