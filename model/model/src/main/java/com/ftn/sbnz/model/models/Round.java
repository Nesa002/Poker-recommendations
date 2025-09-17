package com.ftn.sbnz.model.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.api.definition.type.Position;

public class Round {
    //AA , KJS - King jack suit
    private String hand;

    // na kojoj smo rundi jer uzimamo zadnjih 10 za agresivnost igraca
    private int roundNum;

    // na kojoj smo rundi jer uzimamo zadnjih 10 za agresivnost igraca
    private int playerPosition;
    private String playerName;

    // prati akcije igraca
    private Map<String, List<Integer>> playerActions = new HashMap<>();

    // prati ekonomiju igraca
    private Map<String, Integer> playerChips = new HashMap<>();

    private int currentRaise;
    private int pot;
    private int bigBlindSize;


     // Updated constructor
     public Round(String hand, int currentRoundNum, int playerPosition, String[] players, Integer[] playerChips, int currentRaise, int pot, int bigBlindSize) {
        this.hand = hand;
        this.roundNum = currentRoundNum + 1;
        this.playerPosition = playerPosition;
        this.playerName = players[playerPosition];
        this.currentRaise = currentRaise;
        this.pot = pot;
        this.bigBlindSize = bigBlindSize;

        if (players.length != playerChips.length) {
            throw new IllegalArgumentException("Players and chips arrays must have the same length");
        }

        for (int i = 0; i < players.length; i++) {
            this.playerActions.put(players[i], new ArrayList<>());
            this.playerChips.put(players[i], playerChips[i]);
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

    public void addPlayerActions(String playerPosition, int action) {
        playerActions.get(playerPosition).add(action);
    }

    public void setPlayerActions(Map<String, List<Integer>> playerActions) {
        this.playerActions = playerActions;
    }

    public Map<String, Integer> getPlayerChips() {
        return playerChips;
    }

    public void setPlayerChips(Map<String, Integer> playerChips) {
        this.playerChips = playerChips;
    }

    public double getCurrentRaise() {
        return currentRaise;
    }

    public void setCurrentRaise(int currentRaise) {
        this.currentRaise = currentRaise;
    }

    public int getPot() {
        return pot;
    }

    public void setPot(int pot) {
        this.pot = pot;
    }

    public int getBigBlindSize() {
        return this.bigBlindSize;
    }

    public void setBigBlindSize(int bigBlindSize) {
        this.bigBlindSize = bigBlindSize;
    }

    public Boolean getStrongEconomy() {
        int maxChips = Collections.max(this.playerChips.values());

        return this.playerChips.get(this.playerName).equals(maxChips);
    }

    @Override
    public String toString() {
        return "Round{" +
               "hand='" + hand + '\'' +
               ", roundNum=" + roundNum +
               ", playerPosition=" + playerPosition +
               ", playerActions=" + playerActions +
               ", playerChips=" + playerChips +
               ", currentRaise=" + currentRaise +
               ", pot=" + pot +
               '}';
    }
}
