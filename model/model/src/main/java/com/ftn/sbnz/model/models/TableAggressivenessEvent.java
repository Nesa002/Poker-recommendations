package com.ftn.sbnz.model.models;

import java.util.Date;

public class TableAggressivenessEvent {

    private Date eventTime;

    public TableAggressivenessEvent() {
    }

    public TableAggressivenessEvent(Date eventTime) {
        this.eventTime = eventTime;
    }

    public Date getEventTime() {
        return eventTime;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }

    @Override
    public String toString() {
        return "TableAggressivenessEvent{" +
                "eventTime=" + eventTime +
                '}';
    }
}

