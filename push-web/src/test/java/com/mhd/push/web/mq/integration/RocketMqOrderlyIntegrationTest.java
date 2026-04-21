package com.mhd.push.web.mq.integration;

import com.mhd.push.support.mq.SendMqService;
import com.mhd.push.support.mq.rcoketmq.RocketMqSendMqServiceImpl;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = RocketMqOrderlyIntegrationTest.TestApp.class)
class RocketMqOrderlyIntegrationTest {

    private static final String TOPIC = "it_orderly_scene1_" + System.currentTimeMillis();
    private static final String CONSUMER_GROUP = "it_orderly_group_" + UUID.randomUUID();

    @Autowired
    private SendMqService sendMqService;

    @Autowired
    private OrderlyTestConsumer orderlyTestConsumer;

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("mhd.mq.pipeline", () -> "rocketMq");
        registry.add("rocketmq.name-server", () -> System.getProperty("it.rocketmq.nameserver", "127.0.0.1:9876"));
        registry.add("rocketmq.producer.group", () -> "it-producer-group-" + UUID.randomUUID());
        registry.add("mhd.it.mq.orderly-topic", () -> TOPIC);
        registry.add("mhd.it.mq.orderly-consumer-group", () -> CONSUMER_GROUP);
    }

    @BeforeEach
    void setup() {
        // If MQ is not reachable, skip this integration test to avoid false negatives in CI.
        Assumptions.assumeTrue(isNameServerReachable(nameServer), "RocketMQ name-server is unreachable: " + nameServer);
        orderlyTestConsumer.reset();
    }

    @Test
    // Scenario S1 integration: same business owner + same bizId should keep strict order on orderly consumption.
    void shouldConsumeInOrderForScenarioOne() throws InterruptedException {
        String tag = "mhd";
        String businessOwner = "order-center";
        String bizId = "ORDER-1001";
        String orderKey = businessOwner + ":" + bizId;
        List<String> events = Arrays.asList("cart-success", "order-success", "delivery-success", "receive-success");

        orderlyTestConsumer.expect(events.size());

        for (String event : events) {
            sendMqService.send(TOPIC, event, tag, orderKey);
        }

        boolean completed = orderlyTestConsumer.await(Duration.ofSeconds(20));
        assertTrue(completed, "Timed out waiting for orderly messages");

        List<String> received = orderlyTestConsumer.receivedPayloads();
        assertEquals(events, received, "Order of consumed events must equal send order for scenario one");
    }

    private boolean isNameServerReachable(String nameServerValue) {
        if (nameServerValue == null || nameServerValue.isBlank()) {
            return false;
        }
        String first = nameServerValue.split(";")[0].trim();
        String[] hp = first.split(":");
        if (hp.length != 2) {
            return false;
        }
        String host = hp[0].trim();
        int port;
        try {
            port = Integer.parseInt(hp[1].trim());
        } catch (NumberFormatException ex) {
            return false;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1500);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @SpringBootApplication
    @Import({RocketMqSendMqServiceImpl.class, OrderlyTestConsumer.class})
    static class TestApp {
    }

    @Component
    @RocketMQMessageListener(
            topic = "${mhd.it.mq.orderly-topic}",
            consumerGroup = "${mhd.it.mq.orderly-consumer-group}",
            consumeMode = ConsumeMode.ORDERLY
    )
    static class OrderlyTestConsumer implements RocketMQListener<String> {
        private volatile CountDownLatch latch = new CountDownLatch(0);
        private final CopyOnWriteArrayList<String> payloads = new CopyOnWriteArrayList<>();

        @Override
        public void onMessage(String message) {
            payloads.add(message);
            latch.countDown();
        }

        void expect(int count) {
            this.latch = new CountDownLatch(count);
        }

        boolean await(Duration timeout) throws InterruptedException {
            return latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        void reset() {
            payloads.clear();
            latch = new CountDownLatch(0);
        }

        List<String> receivedPayloads() {
            return List.copyOf(payloads);
        }
    }
}
