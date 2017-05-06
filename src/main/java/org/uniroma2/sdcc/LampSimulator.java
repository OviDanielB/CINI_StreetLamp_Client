package org.uniroma2.sdcc;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.uniroma2.sdcc.Model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Class that connects to RabbitMQ and sends simulated lamp messages
 */
public class LampSimulator
{

    private static String rabbitHost;
    /* rabbit queue name */
    private static String QUEUE_NAME = "storm";

    /* used for random numerical values */
    private static float FAILURE_PROB = .3f;
    private static Float GAUSSIAN_MEAN = 60f;
    private static Float GAUSSIAN_STDEV = 15f;

    private static long counter = 0;

    /* random with fixed seed to simulate again same values */
    private static Random random;
    /* all messages light intensity mean (updated with every message sent) */
    private static float mean;

    /* used for calculating sent message rate*/
    private static MetricRegistry metrics = new MetricRegistry();
    private static Meter requests ;


    public static void main( String[] args )
    {
        random = new Random(12345);
        mean = 0;

        if(args.length == 0){
            System.out.println("Usage: java -jar CINI_StreetLamp_Client-1.0.jar <rabbit host>");
            System.exit(1);
        }
        rabbitHost = args[0];

        startMetrics();

        rabbitProducer();
    }

    /**
     * print message sending rate
     */
    private static void startMetrics() {
        requests = metrics.meter("Sent Tuples");

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(60, TimeUnit.SECONDS);
    }

    /**
     * create a Thread connected to Rabbit that send
     * simulated lamp messages to queue;
     * also updates sending rate and light intensity mean
     */
    private static void rabbitProducer() {

        Thread producer = new Thread(() -> {

            /* rabbit connetion */
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(rabbitHost);
            factory.setPort(5672);

            Connection connection;
            try {
                connection = factory.newConnection();
                Channel channel = connection.createChannel();

                    /* declare queue on which to send messages */
                    channel.queueDeclare(QUEUE_NAME, false, false, false, null);

                    StreetLampMessage streetLamp;
                    Gson gson = new Gson();
                    String message;

                    while (true) {

                        /* random street light */
                        streetLamp = generateRandomStreetLight();

                        /* trasform to json format */
                        message = gson.toJson(streetLamp);

                        /* send json message */
                        channel.basicPublish("", "storm", null, message.getBytes());

                        /* updates sending rate */
                        requests.mark();

                        /* updates number of sent messages and light intensity mean */
                        counter++;
                        updateMean(streetLamp.getStreetLamp().getLightIntensity());
                    }


            } catch (IOException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        });

        producer.run();

    }

    /**
     * online mean updating function
     * @param lightIntensity new value
     */
    private static void updateMean(float lightIntensity) {

        mean = mean + (lightIntensity - mean) / counter;
    }

    /**
     * generate random street light
     * @return street light
     */
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

    /**
     * street mock values
     * @return street name
     */
    private static String randomStreetName() {

        float rand = (float) Math.random();

        if(rand < 0.5){
            return "VIA del POLITECNICO";
        } else {
            return "VIA CAMBRIDGE";
        }
    }

    /**
     * randomly generate malfunctioning lamps (unusual light intensity values )
     * @return true if functionin, false if malfunctioning
     */
    private static boolean randomMalfunctioning() {
        float rand = (float) Math.random();
        if(rand < FAILURE_PROB){
            return false;
        }

        return true;
    }

    /**
     * generates a random float number
     * @return random in [0,100]
     */
    private static float generateRandomFloat() {

        float rand =(float) (Math.random() * 100);
        return rand;
    }

    /**
     * generate random integer
     * @return int in [0, 100000]
     */
    private static int generateRandomInt() {

        int rand = (int) (Math.random() * 100000);
        return rand;
    }
}
