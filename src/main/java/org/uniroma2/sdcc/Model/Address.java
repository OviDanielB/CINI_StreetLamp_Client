package org.uniroma2.sdcc.Model;

import java.io.Serializable;

/**
 * defines a city address
 */
public class Address implements Serializable {

    static final Long serialVersionUID = 1L;


    private String name;
    private int number;
    private AddressNumberType numberType;

    public Address() {
    }

    public Address(String name, int number, AddressNumberType numberType) {
        this.name = name;
        this.number = number;
        this.numberType = numberType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public AddressNumberType getNumberType() {
        return numberType;
    }

    public void setNumberType(AddressNumberType numberType) {
        this.numberType = numberType;
    }

    @Override
    public String toString() {
        return "Address{" +
                "name='" + name + '\'' +
                ", number=" + number +
                ", numberType=" + numberType +
                '}';
    }
}
