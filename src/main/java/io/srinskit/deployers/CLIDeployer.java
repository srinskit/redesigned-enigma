package io.srinskit.deployers;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import io.srinskit.adder.AdderServiceVerticle;
import io.srinskit.apiserver.APIServerVerticle;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.Option;
import io.vertx.core.cli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CLIDeployer {
	private static AbstractVerticle getVerticle(String name) {
		switch (name) {
			case "api-server":
				return new APIServerVerticle();
			case "adder-service":
				return new AdderServiceVerticle();
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

	public static ClusterManager getClusterManager(List<String> zookeepers) {
		JsonObject zkConfig = new JsonObject();
		zkConfig.put("zookeeperHosts", String.join(",", zookeepers));
		
		return new ZookeeperClusterManager(zkConfig);
	}

	public static void deploy(List<String> modules, List<String> zookeepers, String host) {
		ClusterManager mgr = getClusterManager(zookeepers);
		EventBusOptions ebOptions = new EventBusOptions().setClustered(true).setHost(host);
		VertxOptions options = new VertxOptions().setClusterManager(mgr).setEventBusOptions(ebOptions);

		Vertx.clusteredVertx(options, res -> {
			if (res.succeeded()) {
				Vertx vertx = res.result();
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
						.setDescription("modules to launch").addChoice("adder-service").addChoice("api-server"))
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
