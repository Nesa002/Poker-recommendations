package com.ftn.sbnz.model.models;

import java.util.Date;

public class RaiseEvent {
    private String playerId;
    private double amount;
    private Date eventTime;

    public RaiseEvent(String playerId, double amount, Date eventTime) {
        this.playerId = playerId;
        this.amount = amount;
        this.eventTime = eventTime;
    }

    public RaiseEvent() {}

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public Date getEventTime() { return eventTime; }
    public void setEventTime(Date eventTime) { this.eventTime = eventTime; }
}
