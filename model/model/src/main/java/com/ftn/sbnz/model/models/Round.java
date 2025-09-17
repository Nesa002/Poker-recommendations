package com.ftn.sbnz.model.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.api.definition.type.Position;

public class Round {
    //AA , KJS - King jack suit
    @Position(0)
    private String hand;

    // na kojoj smo rundi jer uzimamo zadnjih 10 za agresivnost igraca
    @Position(1)
    private int roundNum;

    // na kojoj smo rundi jer uzimamo zadnjih 10 za agresivnost igraca
    @Position(2)
    private int playerPosition;

    // prati akcije igraca
    private Map<String, List<Integer>> playerActions = new HashMap<>();

    public Round(String hand, int currentRoundNum, int playerPosition, String[] players) {
        this.hand = hand;
        this.roundNum = currentRoundNum+1;

        for (String player: players){
            this.playerActions.put(player, new ArrayList<>());
        }
    }

    // Getters and Setters
    public String getHand() {
        return hand;
    }

    public void setHand(String hand) {
        this.hand = hand;
    }

    public int getRoundNum() {
        return roundNum;
    }

    public void setRoundNum(int roundNum) {
        this.roundNum = roundNum;
    }

    public int getPlayerPosition() {
        return playerPosition;
    }

    public void setPlayerPosition(int playerPosition) {
        this.playerPosition = playerPosition;
    }

    public Map<String, List<Integer>> getPlayerActions() {
        return playerActions;
    }

    public void setPlayerActions(Map<String, List<Integer>> playerActions) {
        this.playerActions = playerActions;
    }

    @Override
    public String toString() {
        return "Hand{" +
               "hand='" + hand + '\'' +
               ", roundNum=" + roundNum +
               ", playerPosition=" + playerPosition +
               ", playerActions=" + playerActions +
               '}';
    }
}
