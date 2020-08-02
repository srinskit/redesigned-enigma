package io.srinskit.deployers;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import java.util.EnumSet;

import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.zookeeper.*;

import io.vertx.micrometer.*;
import io.vertx.core.http.HttpServerOptions;

import io.vertx.core.cli.CLI;
import io.vertx.core.cli.Option;
import io.vertx.core.cli.CommandLine;

import io.srinskit.adder.AdderServiceVerticle;
import io.srinskit.divide.DivideServiceVerticle;
import io.srinskit.apiserver.APIServerVerticle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.vertx.micrometer.*;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.core.instrument.*;
import java.util.EnumSet;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.micrometer.backends.BackendRegistries;

public class CLIDeployer {
	private static AbstractVerticle getVerticle(String name) {
		switch (name) {
			case "api-server":
				return new APIServerVerticle();
			case "adder-service":
				return new AdderServiceVerticle();
			case "divide-service":
				return new DivideServiceVerticle();
		}
		return null;
	}

	public static void recursiveDeploy(Vertx vertx, List<String> modules, int i) {
		if (i >= modules.size()) {
			System.out.println("Deployed all");
			return;
		}
		String module_name = modules.get(i);
		vertx.deployVerticle(getVerticle(module_name), ar -> {
			if (ar.succeeded()) {
				System.out.println("Deployed " + module_name);
				recursiveDeploy(vertx, modules, i + 1);
			} else {
				System.out.println("Failed to deploy " + module_name);
				System.out.println(ar.cause());
			}
		});
	}

	public static ClusterManager getClusterManager(List<String> zookeepers, String clusterID) {
		Config config = new Config();
		config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		config.setProperty("hazelcast.discovery.enabled", "true");

		DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(
				new ZookeeperDiscoveryStrategyFactory());
		discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_URL.key(),
				String.join(",", zookeepers));
		discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.GROUP.key(), clusterID);
		config.getNetworkConfig().getJoin().getDiscoveryConfig().addDiscoveryStrategyConfig(discoveryStrategyConfig);

		return new HazelcastClusterManager(config);
	}

	public static MetricsOptions getMetricsOptions() {
		return new MicrometerMetricsOptions().setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
				.setStartEmbeddedServer(true).setEmbeddedServerOptions(new HttpServerOptions().setPort(9000))
				.setPublishQuantiles(true))
				.setLabels(EnumSet.of(Label.EB_ADDRESS, Label.EB_FAILURE, Label.HTTP_CODE, Label.HTTP_METHOD, Label.HTTP_PATH))
				.setEnabled(true);
	}

	public static void setJVMmetrics() {
		MeterRegistry registry = BackendRegistries.getDefaultNow();
		new JvmMemoryMetrics().bindTo(registry);
		new JvmGcMetrics().bindTo(registry); 
		new ProcessorMetrics().bindTo(registry); 
		new JvmThreadMetrics().bindTo(registry); 
		
	}
	public static void deploy(List<String> modules, List<String> zookeepers, String host) {
		ClusterManager mgr = getClusterManager(zookeepers, "srinskit-calc");
		EventBusOptions ebOptions = new EventBusOptions().setClustered(true).setHost(host);
		VertxOptions options = new VertxOptions().setClusterManager(mgr).setEventBusOptions(ebOptions).setMetricsOptions(getMetricsOptions());

		Vertx.clusteredVertx(options, res -> {
			if (res.succeeded()) {
				Vertx vertx = res.result();
				setJVMmetrics();
				recursiveDeploy(vertx, modules, 0);
			} else {
				System.out.println("Could not join cluster");
			}
		});

	}

	public static void main(String[] args) {
		CLI cli = CLI.create("IUDX RS").setSummary("A CLI to deploy RS components")
				.addOption(
						new Option().setLongName("help").setShortName("h").setFlag(true).setDescription("display help"))
				.addOption(new Option().setLongName("modules").setShortName("m").setMultiValued(true).setRequired(true)
						.setDescription("modules to launch").addChoice("adder-service").addChoice("divide-service").addChoice("api-server"))
				.addOption(new Option().setLongName("zookeepers").setShortName("z").setMultiValued(true)
						.setRequired(true).setDescription("zookeeper hosts"))
				.addOption(new Option().setLongName("host").setShortName("i").setRequired(true)
						.setDescription("public host"));

		StringBuilder usageString = new StringBuilder();
		cli.usage(usageString);
		CommandLine commandLine = cli.parse(Arrays.asList(args), false);
		if (commandLine.isValid() && !commandLine.isFlagEnabled("help")) {
			List<String> modules = new ArrayList<String>(commandLine.getOptionValues("modules"));
			List<String> zookeepers = new ArrayList<String>(commandLine.getOptionValues("zookeepers"));
			String host = commandLine.getOptionValue("host");
			deploy(modules, zookeepers, host);
		} else {
			System.out.println(usageString);
		}
	}

}
