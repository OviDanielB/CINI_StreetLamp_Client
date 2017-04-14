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

    private  static final String  EXCHANGE_NAME = "dashboard_exchange";
    /* topic based pub/sub */
    private  static final String EXCHANGE_TYPE = "topic";
    private  static final String ROUTING_KEY = "dashboard.anomalies";


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

            if(arg.equals("node")){
                factory.setPort(5673);
            }

            String mockMess = "{\"anomalies\":{\"DAMAGED_BULB\":1.0,\"NOT_RESPONDING\":0.0},\"noResponseCount\":0," +
                    "\"streetLamp\":{\"ID\":78569,\"on\":false,\"lampModel\":\"LED\",\"address\":{\"name\":\"POLITECNICO\",\"number\":26828,\"numberType\":\"CIVIC\"}," +
                    "\"lightIntensity\":58.848267,\"consumption\":58.94588,\"lifetime\":{\"date\":{\"year\":2017,\"month\":1,\"day\":19},\"time\":{\"hour\":15,\"minute\":33,\"second\":13,\"nano\":598000000}}}," +
                    "\"timestamp\":1491917600000,\"naturalLightLevel\":76.880745}";

            String mockMess1 = "{\"anomalies\":{\"DAMAGED_BULB\":1.0,\"NOT_RESPONDING\":0.0},\"noResponseCount\":0," +
                    "\"streetLamp\":{\"ID\":12345,\"on\":false,\"lampModel\":\"LED\",\"address\":{\"name\":\"POLITECNICO\",\"number\":26828,\"numberType\":\"CIVIC\"}," +
                    "\"lightIntensity\":58.848267,\"consumption\":58.94588,\"lifetime\":{\"date\":{\"year\":2017,\"month\":1,\"day\":19},\"time\":{\"hour\":15,\"minute\":33,\"second\":13,\"nano\":598000000}}}," +
                    "\"timestamp\":1491917600000,\"naturalLightLevel\":76.880745}";

            String mockMess2 = "{\"anomalies\":{\"DAMAGED_BULB\":1.0,\"NOT_RESPONDING\":0.0},\"noResponseCount\":0," +
                    "\"streetLamp\":{\"ID\":213,\"on\":false,\"lampModel\":\"LED\",\"address\":{\"name\":\"POLITECNICO\",\"number\":26828,\"numberType\":\"CIVIC\"}," +
                    "\"lightIntensity\":60.23423,\"consumption\":45.94588,\"lifetime\":{\"date\":{\"year\":2017,\"month\":1,\"day\":19},\"time\":{\"hour\":15,\"minute\":33,\"second\":13,\"nano\":598000000}}}," +
                    "\"timestamp\":1491917600000,\"naturalLightLevel\":76.880745}";

            String[] mocks = {mockMess,mockMess1,mockMess2};

            Connection connection = null;
            try {
                connection = factory.newConnection();
                Channel channel = connection.createChannel();

                if(arg.equals("node")){

                    channel.exchangeDeclare(EXCHANGE_NAME,EXCHANGE_TYPE);

                    while (true) {

                        Integer r = (int) (Math.random() * 100) % mocks.length;
                        channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, mocks[r].getBytes());
                        Thread.sleep(100);

                    }


                }else {

                    channel.queueDeclare(QUEUE_NAME, false, false, false, null);

                    StreetLampMessage streetLamp;
                    Gson gson = new Gson();
                    String message;
                    while (true) {

                        streetLamp = generateRandomStreetLight();
                        message = gson.toJson(streetLamp);

                        channel.basicPublish("", "storm", null, message.getBytes());


                        counter++;
                        updateMean(streetLamp.getStreetLamp().getLightIntensity());
                        System.out.print(" [CINI] Sent " + counter + " messages with mean = " + mean + "\r");
                        //System.out.println(streetLamp.getNaturalLightLevel().getNaturalLightLevel());
                        Thread.sleep(100);
                    }
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

        // sometimes generate lamp in a specific cell park, other times lamp externally a cell park
        int i = generateRandomInt();
        if ( i % 2 == 0) {
            streetLamp.setCellID(i);
        } else {
            streetLamp.setCellID(-1);
        }



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
        message.setTimestamp(System.currentTimeMillis() - (float)(Math.random() * 1000000));

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
