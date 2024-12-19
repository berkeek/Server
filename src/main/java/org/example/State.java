package org.example;

import java.util.*;

public class State {
    private final Queue<Meme> memes = new LinkedList<>();
    private Meme currentMeme = null;
    private final BinarySemaphore bs = new BinarySemaphore(true);
    public static final Set<String> votedClients = new HashSet<>();
    private final List<Meme> leaderboard = new ArrayList<>();

    //leaderboard is written and read by one thread only, therefore no lock needed
    //currentMeme, memes, and votedClients are accessed from different threads
    //mutex and synchronization are used for those shared variables in the state

    public List<Meme> getLeaderboard() {
        return leaderboard;
    }


    public Meme getCurrentMeme() {
        bs.P();
        try{
            return currentMeme;
        }finally{
            bs.V();
        }
    }

    //write to memes
    public void addMeme(Meme meme) {
        bs.P();
        try{
            memes.add(meme);
            System.out.println("Meme added to queue: " + meme.getName());
        }finally{
            bs.V();
        }
    }

    public void addVote(int vote) {
        bs.P();
        try{
            if (currentMeme == null) {
                System.out.println("There are no memes to vote");
                return;
            }
            currentMeme.addVote(vote);
        }finally{
            bs.V();
        }
    }

    public void updateMeme(){
        synchronized (votedClients) {
            bs.P();
            try{
                Meme meme = memes.poll();
                votedClients.clear();

                if (meme != null) {
                    currentMeme = meme;
                    System.out.println("Meme updated: " + meme.getName());
                }
                else {
                    System.out.println("There are no memes in the queue");
                    currentMeme = null;
                }
            }finally {
                bs.V();
            }
        }
    }

    public void updateLeaderboard(){
        bs.P();
        try{
            if(currentMeme == null){
                System.out.println("No current meme to evaluate for leaderboard.");
                return;
            }

            double currentAverage = currentMeme.calculateAverage();

            // Add directly if not enough memes on leaderboard
            if (leaderboard.size() < 3) {

                leaderboard.add(currentMeme);
                System.out.println("Added to leaderboard: " + currentMeme.getName() + " with average " + currentAverage);
            } else {
                // Find the meme with the lowest average in the leaderboard
                Meme thirdPlaceMeme = leaderboard.get(leaderboard.size() - 1);
                double thirdPlaceAverage = thirdPlaceMeme.calculateAverage();

                if (currentAverage > thirdPlaceAverage) {
                    // Replace the third meme with the current meme
                    leaderboard.remove(leaderboard.size() - 1);
                    leaderboard.add(currentMeme);
                    System.out.println("Replaced '" + thirdPlaceMeme.getName() + "' with '" + currentMeme.getName() + "' in leaderboard.");
                } else {
                    System.out.println("Meme '" + currentMeme.getName() + "' did not qualify for leaderboard.");
                }
            }

            // Sort the leaderboard in descending order of average votes
            leaderboard.sort((m1, m2) -> Double.compare(m2.calculateAverage(), m1.calculateAverage()));

            // Ensure leaderboard size does not exceed 3
            if (leaderboard.size() > 3) {
                leaderboard.subList(3, leaderboard.size()).clear();
            }

        } finally{
            bs.V();
        }
    }
}
