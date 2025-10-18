package com.ftn.sbnz.model.models;

import org.kie.api.definition.type.Position;

public class HandCategory {

    @Position(0)
    private String hand;
    
    @Position(1)
    private String category;

    public HandCategory(String hand, String category) {
        this.hand = hand;
        this.category = category;
    }

    public String getHand() { return hand; }
    public void setHand(String hand) { this.hand = hand; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HandCategory that = (HandCategory) o;
        return java.util.Objects.equals(hand, that.hand) &&
               java.util.Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(hand, category);
    }
}