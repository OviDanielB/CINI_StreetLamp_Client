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
    private static String arg1;

    private  static final String  EXCHANGE_NAME = "dashboard_exchange";
    /* topic based pub/sub */
    private  static final String EXCHANGE_TYPE = "topic";
    private  static final String ROUTING_KEY = "dashboard.anomalies";


    public static void main( String[] args )
    {

        arg = args[0];
        if(args.length == 2) {
            arg1 = args[1];
        } else {
            arg1 = "";
        }

        random = new Random(12345);
        mean = 0;


        rabbitProducer();
    }

    private static void rabbitProducer() {

        Thread producer = new Thread(() -> {

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("Helios-ELB-855796889.eu-west-1.elb.amazonaws.com");
            factory.setPort(5672);

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

                        switch (arg1){
                            case "dashboard.statistics.lamps":
                                String statLamp = "{\"id\":50,\"street\":\"Via Maggio\",\"consumption\":0.0018343345," +
                                        "\"timestamp\":{\"date\":{\"year\":2017,\"month\":4,\"day\":12}," +
                                        "\"time\":{\"hour\":15,\"minute\":33,\"second\":0,\"nano\":0}},\"window length\":5}";
                                channel.basicPublish(EXCHANGE_NAME, arg1, null, statLamp.getBytes());
                                System.out.println(statLamp + "\r");
                                break;

                            case "dashboard.statistics.streets":
                                String statStreet = "{\"street\":\"Piazza Risorgimento\",\"consumption\":0.16939843," +
                                        "\"timestamp\":{\"date\":{\"year\":2017,\"month\":4,\"day\":12}," +
                                        "\"time\":{\"hour\":15,\"minute\":33,\"second\":0,\"nano\":0}},\"window length\":5}";
                                channel.basicPublish(EXCHANGE_NAME, arg1, null, statStreet.getBytes());
                                System.out.println(statStreet + "\r");
                                break;

                            case "dashboard.statistics.global":
                                String statGlobal = "{\"street\":\"*\",\"consumption\":0.07858789," +
                                        "\"timestamp\":{\"date\":{\"year\":2017,\"month\":4,\"day\":12}," +
                                        "\"time\":{\"hour\":16,\"minute\":10,\"second\":0,\"nano\":0}},\"window length\":14}";
                                channel.basicPublish(EXCHANGE_NAME, arg1, null, statGlobal.getBytes());
                                System.out.println(statGlobal + "\r");

                                break;

                            case "dashboard.rank":
                                String rank = " {\"ranking\":[" +
                                        "{\"id\":36331,\"address\":{\"name\":\"CAMBRIDGE\",\"number\":37794,\"numberType\":\"CIVIC\"},\"lifetime\":{\"date\":{\"year\":2017,\"month\":2,\"day\":15},\"time\":{\"hour\":17,\"minute\":8,\"second\":38,\"nano\":881000000}},\"timestamp\":1492095730000}," +
                                        "{\"id\":24801,\"address\":{\"name\":\"POLITECNICO\",\"number\":54762,\"numberType\":\"CIVIC\"},\"lifetime\":{\"date\":{\"year\":2017,\"month\":2,\"day\":16},\"time\":{\"hour\":17,\"minute\":8,\"second\":36,\"nano\":287000000}},\"timestamp\":1492095860000}," +
                                        "{\"id\":84057,\"address\":{\"name\":\"CAMBRIDGE\",\"number\":48838,\"numberType\":\"CIVIC\"},\"lifetime\":{\"date\":{\"year\":2017,\"month\":2,\"day\":17},\"time\":{\"hour\":17,\"minute\":8,\"second\":27,\"nano\":177000000}},\"timestamp\":1492095990000}," +
                                        "{\"id\":22190,\"address\":{\"name\":\"POLITECNICO\",\"number\":80962,\"numberType\":\"CIVIC\"},\"lifetime\":{\"date\":{\"year\":2017,\"month\":2,\"day\":17},\"time\":{\"hour\":17,\"minute\":8,\"second\":28,\"nano\":310000000}},\"timestamp\":1492095990000}," +
                                        "{\"id\":15180,\"address\":{\"name\":\"CAMBRIDGE\",\"number\":18736,\"numberType\":\"CIVIC\"},\"lifetime\":{\"date\":{\"year\":2017,\"month\":2,\"day\":18},\"time\":{\"hour\":17,\"minute\":8,\"second\":35,\"nano\":455000000}},\"timestamp\":1492095070000}," +
                                        "{\"id\":87815,\"address\":{\"name\":\"CAMBRIDGE\",\"number\":78779,\"numberType\":\"CIVIC\"},\"lifetime\":{\"date\":{\"year\":2017,\"month\":2,\"day\":20},\"time\":{\"hour\":17,\"minute\":8,\"second\":25,\"nano\":304000000}},\"timestamp\":1492095600000}," +
                                        "{\"id\":85020,\"address\":{\"name\":\"POLITECNICO\",\"number\":36266,\"numberType\":\"CIVIC\"},\"lifetime\":{\"date\":{\"year\":2017,\"month\":2,\"day\":20},\"time\":{\"hour\":17,\"minute\":8,\"second\":37,\"nano\":119000000}},\"timestamp\":1492095730000}," +
                                        "{\"id\":80561,\"address\":{\"name\":\"POLITECNICO\",\"number\":53440,\"numberType\":\"CIVIC\"},\"lifetime\":{\"date\":{\"year\":2017,\"month\":2,\"day\":21},\"time\":{\"hour\":17,\"minute\":8,\"second\":17,\"nano\":922000000}},\"timestamp\":1492095990000}," +
                                        "{\"id\":48373,\"address\":{\"name\":\"CAMBRIDGE\",\"number\":9804,\"numberType\":\"CIVIC\"},\"lifetime\":{\"date\":{\"year\":2017,\"month\":2,\"day\":21},\"time\":{\"hour\":17,\"minute\":8,\"second\":23,\"nano\":226000000}},\"timestamp\":1492096120000}," +
                                        "{\"id\":26771,\"address\":{\"name\":\"POLITECNICO\",\"number\":22462,\"numberType\":\"CIVIC\"},\"lifetime\":{\"date\":{\"year\":2017,\"month\":2,\"day\":21},\"time\":{\"hour\":17,\"minute\":8,\"second\":24,\"nano\":687000000}},\"timestamp\":1492095600000}]," +
                                        "\"count\":235}\n";
                                channel.basicPublish(EXCHANGE_NAME, arg1, null, rank.getBytes());
                                System.out.println(rank + "\r");
                            default:
                                Integer r = (int) (Math.random() * 100) % mocks.length;
                                channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, mocks[r].getBytes());
                        }

                        //Thread.sleep(1000);

                    }


                }else {

                    channel.queueDeclare(QUEUE_NAME, false, false, false, null);

                    StreetLampMessage streetLamp;
                    Gson gson = new Gson();
                    String message;

                    streetLamp = generateRandomStreetLight();

                    if(arg.equals("anomaly")){
                        streetLamp.getStreetLamp().setID(Integer.valueOf(arg1));
                        streetLamp.getStreetLamp().setLightIntensity(20);
                    }

                    while (true) {

                        Float newValue = streetLamp.getStreetLamp().getLightIntensity() ;

                        if(arg.equals("anomaly")){

                        }else {
                         newValue = streetLamp.getStreetLamp().getLightIntensity() + generateRandomIntNegPos();
                            if(newValue > 100 )
                            { newValue = newValue - 20;} else if(newValue < 40 ){newValue = newValue + 40;}
                        }



                        streetLamp.getStreetLamp().setLightIntensity(newValue);
                        streetLamp.setTimestamp(System.currentTimeMillis() -  1000000);
                        //streetLamp.setNaturalLightLevel(generateRandomFloatGaussian());
                        message = gson.toJson(streetLamp);

                        channel.basicPublish("", "storm", null, message.getBytes());


                        counter++;
                        updateMean(streetLamp.getStreetLamp().getLightIntensity());
                        //System.out.print(" [CINI] Sent " + counter + " messages with mean = " + mean + "\r");
                        //System.out.println(message);
                        Thread.sleep(1);
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

    private static float generateRandomIntNegPos() {

        return (float) (Math.random()*2 - 1);
    }

    private static void updateMean(float lightIntensity) {

        mean = mean + (lightIntensity - mean) / counter;
    }

    private static StreetLampMessage generateRandomStreetLight() {
        Address address = new Address();
        //address.setAddressType(AddressType.STREET);
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
