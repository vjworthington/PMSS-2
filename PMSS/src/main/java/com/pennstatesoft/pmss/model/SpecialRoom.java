package com.pennstatesoft.pmss.model;

public class SpecialRoom extends Room {

    private double fee;

    public SpecialRoom(int roomNumber) {
        super(roomNumber);
        fee = 100.00;
    }

    public double getFee(){
        return fee;
    }
    public void setFee(double fee){ this.fee = fee; }

}
