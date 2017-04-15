package org.uniroma2.sdcc;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import org.uniroma2.sdcc.Model.*;
import org.uniroma2.sdcc.Model.Address;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
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

    private static String testNumber = "test1";
    private static volatile boolean doingTest = false;



    public static void main(String[] args){

        random = new Random(12345);
        gson = new Gson();

        connectToRabbits();

        if(args.length != 2){
            logToScreen("Usage: java -jar TopologyTest <topologyName> test<test number>");
            logToScreen("topologyName = anomaly, statistics, ranking");
            System.exit(1);
        }

        topologyName = args[0];
        testNumber = args[1];

        switch (topologyName){
            case "anomaly":
                logToScreen("Starting Anomaly Topology Testing");
                switch (testNumber){
                    case "test1":
                        anomalyTest1();
                        break;
                    case "test2":
                        anomalyTest2();
                        break;
                    default:
                        logFail("No Such Test. Try again with test1, test2 ...");
                        System.exit(1);
                }


                break;
            case "statistics":
                logToScreen("Starting Statistics Topology Testing");

                break;

            case "ranking":
                logToScreen("Starting Ranking Topology Testing");

                break;

        }



    }

    /**
     * sends 4 different lamp messages, 3 with same
     * lamp state (ON/OFF) and waits for result on the end queue;
     * tests if the mess with ID the one with different state
     * has BULB_DAMAGE anomaly
     */
    private static void anomalyTest2() {
        waitForTest();
        beginTest();
        logToScreen("Starting  " + TermCol.ANSI_BLUE + "Anomaly Topology Test 2" + TermCol.ANSI_RESET);

        StreetLampMessage lamp1 = generateRandomStreetLight();
        StreetLampMessage lamp2 = generateRandomStreetLight();
        lamp2.getStreetLamp().setOn(lamp1.getStreetLamp().isOn());

        StreetLampMessage lamp3 = generateRandomStreetLight();
        lamp3.getStreetLamp().setOn(lamp1.getStreetLamp().isOn());

        StreetLampMessage lamp4 = generateRandomStreetLight();
        lamp4.getStreetLamp().setOn(!lamp1.getStreetLamp().isOn());

        Consumer consumer = new DefaultConsumer(channelExit){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");

                AnomalyStreetLampMessage anomalyMess = gson.fromJson(message,AnomalyStreetLampMessage.class);
                if(envelope.getRoutingKey().equals("dashboard.anomalies")){
                    logToScreen("Received " + message);
                }

                if(anomalyMess.getStreetLamp().getID() == lamp4.getStreetLamp().getID()){
                    if(anomalyMess.getAnomalies().get(MalfunctionType.DAMAGED_BULB) != null){
                        logOK("Anomaly Test 2 OK");
                    } else {
                        logFail("Anomaly Test 2 FAILED");
                    }
                }

                endTest();
            }
        };



        try {
            channelExit.basicConsume(EXIT_QUEUE_NAME, false, consumer);
            channelEntry.basicPublish("", "storm", null, gson.toJson(lamp1).getBytes());
            channelEntry.basicPublish("", "storm", null, gson.toJson(lamp2).getBytes());
            channelEntry.basicPublish("", "storm", null, gson.toJson(lamp3).getBytes());
            channelEntry.basicPublish("", "storm", null, gson.toJson(lamp4).getBytes());
            Thread.sleep(1000);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


    }

    private static void waitForTest() {
        while (doingTest){
            logToScreen("WAITING");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * tests whether a message sent to Anomaly Detection Topology
     * returns after a while with unchanged lamp values (ID,light level,address,etc)
     * but also that has a NOT_RESPONDING anomaly since it sent only a message and no other
     * after. it also tests if a message anomaly is redirected on the correct
     * topic (dashboard.anomalies) on the final rabbit queue
     */
    private static void anomalyTest1() {
        beginTest();
        logToScreen("Starting  " + TermCol.ANSI_BLUE + "Anomaly Topology Test 1" + TermCol.ANSI_RESET);


        StreetLampMessage lampMessage = generateRandomStreetLight();
        String json = gson.toJson(lampMessage);
        logToScreen(json);

        Consumer consumer = new DefaultConsumer(channelExit) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                logToScreen("Received : " + message);

                if(envelope.getRoutingKey().equals("dashboard.anomalies")){

                    AnomalyStreetLampMessage anomalyMess = gson.fromJson(message,AnomalyStreetLampMessage.class);

                    boolean correctID;
                    if (anomalyMess.getStreetLamp().getID() == lampMessage.getStreetLamp().getID()) correctID = true;
                    else correctID = false;

                    boolean correctAnomaly = anomalyMess.getAnomalies().get(MalfunctionType.NOT_RESPONDING) != null;
                    boolean unchangedLightLevel = Objects.equals(anomalyMess.getNaturalLightLevel(), lampMessage.getNaturalLightLevel());
                    boolean unchangedAddress =  lampMessage.getStreetLamp().getAddress().getName().equals(anomalyMess.getStreetLamp().getAddress().getName());

                    if(correctID && correctAnomaly && unchangedLightLevel && unchangedAddress) logOK("Anomaly Test 1 OK");
                    else logFail("Anomaly Test 1 FAILED");

                    endTest();
                }
            }
        };


        try {
            channelExit.basicConsume(EXIT_QUEUE_NAME, false, consumer);
            channelEntry.basicPublish("", "storm", null, json.getBytes());
            Thread.sleep(1000);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * exit after every test
     */
    private static void endTest() {
        doingTest = false;
        logToScreen("END");
        System.exit(0);
    }

    /* at the beginning of every test */
    private static void beginTest() {
        doingTest = true;
    }

    /**
     * connect to first queue and last on the topology
     */
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

    private static void logOK(String s){
        System.out.println(TermCol.ANSI_GREEN + LOG_TAG + s + TermCol.ANSI_RESET);
    }

    private static void logFail(String s){
        System.out.println(TermCol.ANSI_RED + LOG_TAG + s + TermCol.ANSI_RESET);
    }


    private static StreetLampMessage generateRandomStreetLight() {
        Address address = new Address();
        address.setName("Via Politecnico");
        address.setNumber(11);
        /*
        address.setName(randomStreetName());
        address.setNumber(generateRandomInt());
        */
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

    private class TermCol{
        public static final String ANSI_RESET = "\u001B[0m";
        public static final String ANSI_BLACK = "\u001B[30m";
        public static final String ANSI_RED = "\u001B[31m";
        public static final String ANSI_GREEN = "\u001B[32m";
        public static final String ANSI_YELLOW = "\u001B[33m";
        public static final String ANSI_BLUE = "\u001B[34m";
        public static final String ANSI_PURPLE = "\u001B[35m";
        public static final String ANSI_CYAN = "\u001B[36m";
        public static final String ANSI_WHITE = "\u001B[37m";
    }
}
