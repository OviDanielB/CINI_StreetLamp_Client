package org.uniroma2.sdcc;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import org.uniroma2.sdcc.Model.*;
import org.uniroma2.sdcc.Model.Address;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 * Created by ovidiudanielbarba on 15/04/2017.
 */
public class StormTopologiesTest {

    private static final String LOG_TAG = "[CINI][Topology Test] ";

    /* rabbit topology entry connection */
    private static Connection connectionEntry;
    private static Channel channelEntry;
    private static final String RABBIT_ENTRY_HOST = "localhost";
    private static final Integer RABBIT_ENTRY_PORT = 5672;
    private static final String RABBIT_ENTRY_QUEUE_NAME = "test";

    /* rabbit topology exit connection */
    private static Connection connectionExit;
    private static Channel channelExit;
    private static final String RABBIT_EXIT_HOST = "localhost";
    private static final Integer RABBIT_EXIT_PORT = 5673;
    private  static final String  EXIT_EXCHANGE_NAME = "dashboard_exchange";
    private static String EXIT_QUEUE_NAME ;
    private static final String[] routingKeys = {"dashboard.anomalies", "dashboard.rank" , "dashboard.statistics.lamps","dashboard.statistics.streets",
                                                    "dashboard.statistics.global"};
    /* topic based pub/sub */
    private  static final String EXIT_EXCHANGE_TYPE = "topic";
    private static final String EXIT_ROUTING_KEY_BASE = "dashboard."; //to be completed for different topologies
    private static Consumer exitConsumer;


    private static Random random;
    private static float FAILURE_PROB = .3f;
    private static Float GAUSSIAN_MEAN = 60f;
    private static Float GAUSSIAN_STDEV = 15f;

    /* json converter */
    private static Gson gson;

    private static String topologyName = "NONE";



    public static void main(String[] args){

        random = new Random(12345);
        gson = new Gson();

        connectToRabbits();

        if(args.length != 1){
            logToScreen("Usage: java -jar TopologyTest <topologyName>");
            logToScreen("topologyName = anomaly, statistics, ranking");
            System.exit(1);
        }

        topologyName = args[0];

        switch (topologyName){
            case "anomaly":
                logToScreen("Starting Anomaly Topology Testing");
                anomalyTest1();
                break;
            case "statistics":
                logToScreen("Starting Statistics Topology Testing");

                break;

            case "ranking":
                logToScreen("Starting Ranking Topology Testing");

                break;

        }



    }

    private static void anomalyTest1() {
        logToScreen("Starting anomaly Test 1");

        StreetLampMessage lampMessage = generateRandomStreetLight();
        String json = gson.toJson(lampMessage);

        Consumer consumer = new DefaultConsumer(channelExit) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                logToScreen("Received : " + message);
            }
        };


        try {
            channelExit.basicConsume(EXIT_QUEUE_NAME, false, consumer);

            while (true) {
                channelEntry.basicPublish("", "storm", null, json.getBytes());
                logToScreen(json);
                Thread.sleep(1000);
            }



        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

        }

    }

    private static void connectToRabbits() {
        /* entry */
        ConnectionFactory factoryEntry = new ConnectionFactory();
        factoryEntry.setHost(RABBIT_ENTRY_HOST);
        factoryEntry.setPort(RABBIT_ENTRY_PORT);

        /* exit */
        ConnectionFactory factoryExit = new ConnectionFactory();
        factoryExit.setHost(RABBIT_EXIT_HOST);
        factoryExit.setPort(RABBIT_EXIT_PORT);

        try {
            connectionEntry = factoryEntry.newConnection();
            channelEntry = connectionEntry.createChannel();
            channelEntry.queueDeclare(RABBIT_ENTRY_QUEUE_NAME,false,false,false,null);


            connectionExit = factoryExit.newConnection();
            channelExit = connectionExit.createChannel();
            channelExit.exchangeDeclare(EXIT_EXCHANGE_NAME,EXIT_EXCHANGE_TYPE);

            EXIT_QUEUE_NAME = channelExit.queueDeclare().getQueue();

            for(String k : routingKeys){
                channelExit.queueBind(EXIT_QUEUE_NAME,EXIT_EXCHANGE_NAME,k);
            }


        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            logToScreen("Rabbit Connection Failed");
        }
    }

    private static void logToScreen(String s) {
        System.out.println(LOG_TAG + s);
    }

    private static StreetLampMessage generateRandomStreetLight() {
        Address address = new Address();
        address.setName(randomStreetName());
        address.setNumber(generateRandomInt());
        address.setNumberType(AddressNumberType.CIVIC);

        StreetLamp streetLamp = new StreetLamp();
        streetLamp.setAddress(address);

        // sometimes generate lamp in a specific cell park, other times lamp externally a cell park
        int i = generateRandomInt();
        if ( i % 2 == 0) {
            streetLamp.setCellID(i);
        } else {
            streetLamp.setCellID(-1);
        }

        streetLamp.setID(generateRandomInt());
        streetLamp.setLightIntensity(generateRandomFloatGaussian());

        streetLamp.setLampModel(Lamp.LED);
        streetLamp.setCellID(generateRandomInt());
        streetLamp.setOn(randomMalfunctioning());
        streetLamp.setConsumption(generateRandomFloat());
        streetLamp.setLifetime(LocalDateTime.now().minus(generateRandomInt() % 100, ChronoUnit.DAYS));

        StreetLampMessage message = new StreetLampMessage();
        message.setNaturalLightLevel(generateRandomFloat());
        message.setStreetLamp(streetLamp);
        message.setTimestamp(System.currentTimeMillis() - (long) (Math.random() * 1000000));

        return message;
    }


    /**
     * Normal(GAUSSIAN_MEAN,GAUSSIAN_STDEV)
     * @return random float from Normal dist with
     * mean GAUSSIAN_MEAN and stdev GAUSSIAN_STDEV
     */
    private static float generateRandomFloatGaussian() {



        return (float) random.nextGaussian() * GAUSSIAN_STDEV + GAUSSIAN_MEAN;

    }


    private static String randomStreetName() {


        float rand = (float) Math.random();

        if(rand < 0.5){
            return "VIA del POLITECNICO";
        } else {
            return "VIA CAMBRIDGE";
        }
    }

    private static boolean randomMalfunctioning() {
        float rand = (float) Math.random();
        if(rand < FAILURE_PROB){
            return false;
        }

        return true;
    }

    private static float generateRandomFloat() {

        float rand =(float) (Math.random() * 100);
        return rand;
    }

    private static int generateRandomInt() {

        int rand = (int) (Math.random() * 100000);
        return rand;
    }
}
