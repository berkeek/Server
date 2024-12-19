package org.example;

import java.util.ArrayList;

public class Meme {
    private final int id;
    private final String name;
    private final byte[] image;
    private final ArrayList<Integer> votes;

    public Meme(int id, String name, byte[] image) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.votes = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public byte[] getImage() {
        return image;
    }

    public void addVote(int vote) {
        synchronized (votes) {
            votes.add(vote);
        }
    }

    public double calculateAverage() {
        synchronized (votes) {
            return votes.stream()
                    .mapToDouble(d -> d)
                    .average()
                    .orElse(0.0);
        }
    }
}

