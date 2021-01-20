package io.srinskit.deployers;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import java.util.EnumSet;

import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.zookeeper.*;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.Option;
import io.vertx.core.cli.CommandLine;
import io.srinskit.adder.AdderServiceVerticle;
import io.srinskit.divider.DividerServiceVerticle;
import io.srinskit.apiserver.APIServerVerticle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch; 
import java.util.concurrent.TimeUnit;

import io.vertx.core.metrics.MetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.Label;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.micrometer.backends.BackendRegistries;
// JVM metrics imports
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CLIDeployer {
	private static final Logger LOGGER = LogManager.getLogger(CLIDeployer.class);
	private static ClusterManager mgr;
	private static Vertx vertx;
	private static AbstractVerticle getVerticle(String name) {
		switch (name) {
			case "api-server":
				return new APIServerVerticle();
			case "adder-service":
				return new AdderServiceVerticle();
			case "divider-service":
				return new DividerServiceVerticle();
		}
		return null;
	}

	public static void recursiveDeploy(Vertx vertx, List<String> modules, int i) {
		if (i >= modules.size()) {
			LOGGER.info("Deployed all");
			return;
		}
		String module_name = modules.get(i);
		vertx.deployVerticle(getVerticle(module_name), ar -> {
			if (ar.succeeded()) {
				LOGGER.info("Deployed " + module_name);
				recursiveDeploy(vertx, modules, i + 1);
			} else {
				// System.out.println("Failed to deploy " + module_name);
				LOGGER.error("Failed to deploy " + module_name + "cause:",ar.cause());
				// System.out.println(ar.cause());
			}
		});
	}

	public static ClusterManager getClusterManager(String host, List<String> zookeepers, String clusterID) {
		Config config = new Config();
		config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		config.getNetworkConfig().setPublicAddress(host);
		config.setProperty("hazelcast.discovery.enabled", "true");
		config.setProperty("hazelcast.logging.type","log4j2");
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
				.setStartEmbeddedServer(true).setEmbeddedServerOptions(new HttpServerOptions().setPort(9000)))
				// .setPublishQuantiles(true))
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
		mgr = getClusterManager(host, zookeepers, "srinskit-calc");
		EventBusOptions ebOptions = new EventBusOptions().setClustered(true).setHost(host);
		VertxOptions options = new VertxOptions().setClusterManager(mgr).setEventBusOptions(ebOptions).setMetricsOptions(getMetricsOptions());

		Vertx.clusteredVertx(options, res -> {
			if (res.succeeded()) {
				vertx = res.result();
				setJVMmetrics();
				recursiveDeploy(vertx, modules, 0);
				
			} else {
				// System.out.println("Could not join cluster");
				LOGGER.error("Could not join cluster");
				
			}
		});

	}

	public static void gracefulShutdown() {
		Set <String> deployIDSet=vertx.deploymentIDs();
		System.out.println("number of verticles being undeployed are:"+ deployIDSet.size());
		CountDownLatch latch_verticles = new CountDownLatch(deployIDSet.size()); 
		CountDownLatch latch_cluster = new CountDownLatch(1); 
		CountDownLatch latch_vertx = new CountDownLatch(1);
		for (String deploymentID : deployIDSet) {
			vertx.undeploy(deploymentID, res -> {
			if (res.succeeded()) {
				LOGGER.info(deploymentID+" verticle  successfully Undeployed");
				latch_verticles.countDown();
			} else {
				LOGGER.error(deploymentID+ "Undeploy failed!");
			}

		});
			
		
		}
		try { 
			latch_verticles.await(5, TimeUnit.SECONDS);
			mgr.leave(prom->{
				if(prom.succeeded()){							
					System.out.println("Hazelcast succesfully left:"+prom.result());
					latch_cluster.countDown();
									
				}
				else
				{
					
				System.out.println("Error while hazelcast leaving:"+ prom.cause());
				}
			});
		}
		catch(Exception e) {
				e.printStackTrace();
		}

		try {
			latch_cluster.await(5, TimeUnit.SECONDS);
			System.out.println("Closing vertx");		
			vertx.close(prom1->{
				if(prom1.succeeded()){
					System.out.println("vertx closed succesfully:"+prom1.result());
					latch_vertx.countDown();		
				}
				else
				{
					System.out.println("Error vertx didn't close properly, reason:"+ prom1.cause());
					
				}
			});
			

		} 
		catch(Exception e) {
			e.printStackTrace();
		}

		try {
			latch_vertx.await(5, TimeUnit.SECONDS);
		}
		
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	public static void main(String[] args) {
		CLI cli = CLI.create("IUDX RS").setSummary("A CLI to deploy RS components")
				.addOption(
						new Option().setLongName("help").setShortName("h").setFlag(true).setDescription("display help"))
				.addOption(new Option().setLongName("modules").setShortName("m").setMultiValued(true).setRequired(true)
						.setDescription("modules to launch").addChoice("adder-service").addChoice("divider-service").addChoice("api-server"))
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
			Runtime.getRuntime().addShutdownHook(new Thread(() -> gracefulShutdown()));		
		} else {
			System.out.println(usageString);
		}
	}

}
