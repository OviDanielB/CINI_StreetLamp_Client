package org.uniroma2.sdcc;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.uniroma2.sdcc.Model.*;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 * Hello world!
 *
 */
public class App 
{

    private static String QUEUE_NAME = "storm";
    private static float FAILURE_PROB = .3f;

    private static Float GAUSSIAN_MEAN = 60f;
    private static Float GAUSSIAN_STDEV = 15f;

    private static long counter = 0;

    private static Random random;
    private static float mean;

    private static String arg;


    public static void main( String[] args )
    {

        arg = args[0];

        random = new Random(12345);
        mean = 0;


        rabbitProducer();
    }

    private static void rabbitProducer() {

        Thread producer = new Thread(() -> {

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = null;
            try {
                connection = factory.newConnection();
                Channel channel = connection.createChannel();

                channel.queueDeclare(QUEUE_NAME, false, false, false, null);

                StreetLampMessage streetLamp ;
                Gson gson = new Gson();
                String message;
                while(true) {

                    streetLamp = generateRandomStreetLight();
                    message = gson.toJson(streetLamp);
                    channel.basicPublish("", "storm", null, message.getBytes());

                    counter++;
                    updateMean(streetLamp.getStreetLamp().getLightIntensity());
                    System.out.print(" [CINI] Sent " + counter + " messages with mean = " + mean + "\r");
                    //System.out.println(streetLamp.getNaturalLightLevel().getNaturalLightLevel());

                    
                    Thread.sleep(100);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        producer.run();



    }

    private static void updateMean(float lightIntensity) {

        mean = mean + (lightIntensity - mean) / counter;
    }

    private static StreetLampMessage generateRandomStreetLight() {
        Address address = new Address();
        address.setAddressType(AddressType.STREET);
        address.setName(randomStreetName());
        address.setNumber(generateRandomInt());
        address.setNumberType(AddressNumberType.CIVIC);

        StreetLamp streetLamp = new StreetLamp();
        streetLamp.setAddress(address);



        if(arg.equals("anomaly")){
            streetLamp.setID(12345);
            streetLamp.setLightIntensity(90f);

        }else {
            streetLamp.setID(generateRandomInt());
            streetLamp.setLightIntensity(generateRandomFloatGaussian());
        }


        streetLamp.setLampModel(Lamp.LED);
        streetLamp.setOn(randomMalfunctioning());
        streetLamp.setConsumption(generateRandomFloat());
        streetLamp.setLifetime(LocalDateTime.now().minus(generateRandomInt() % 100, ChronoUnit.DAYS));
        
        StreetLampMessage message = new StreetLampMessage();
        message.setNaturalLightLevel(generateRandomFloat());
        message.setStreetLamp(streetLamp);
        message.setTimestamp(System.currentTimeMillis());

        return message;
    }

    private static float mockFloatValues(String type) {

        List<Float> values = new ArrayList<>();
        switch (type){
            case "normal":
                values.add(59f);
                values.add(60f);
                values.add(61f);
                values.add(64f);
                values.add(54f);
                values.add(69f);
                values.add(60f);
                values.add(60f);
                values.add(70f);
                values.add(71f);
                values.add(62f);
                values.add(65f);
                break;
            case "anomaly":
                values.add(90f);
                values.add(89f);
                values.add(82f);
        }

        return values.get((int) (Math.random() * 100 % values.size()));

    }

    /**
     * Normal(GAUSSIAN_MEAN,GAUSSIAN_STDEV)
     * @return random float from Normal dist with
     * mean GAUSSIAN_MEAN and stdev GAUSSIAN_STDEV
     */
    private static float generateRandomFloatGaussian() {



        return (float) random.nextGaussian() * GAUSSIAN_STDEV + GAUSSIAN_MEAN;

        /* 1299721 is prime
        if(System.currentTimeMillis() % 1299721 == 0) {
            System.out.println("[CINI] INTENSITY ANOMALY!!!");
            return 100f;
        } else {


        }
        */
    }

    private static String randomStreetName() {


        float rand = (float) Math.random();

        if(rand < 0.5){
            return "POLITECNICO";
        } else {
            return "CAMBRIDGE";
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
