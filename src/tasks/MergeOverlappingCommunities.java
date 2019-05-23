package tasks;

import graph.Graph;
import graph.InducedSubgraph;
import index.InverseIndex;
import io.GraphReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class MergeOverlappingCommunities {
    private static long stime = System.currentTimeMillis();
    private static int numMerged = 0;
    private static int numPairs = 0;

    private static final double edgeOverlapThreshold = 2;
    private static final double nodeOverlapThreshold = 0.5;
    private static final double evDeltaThreshold = 0.01;

    private static int numThreads;

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length != 1) {
            System.out.println("usage: MergeOverlappingCommunities numThreads");
        } else
            numThreads = Integer.parseInt(args[0]);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            float runtime = (System.currentTimeMillis() - stime) / 1000f;
            System.out.println("runtime: " + runtime);
            System.out.println("pairs: " + numPairs);
            System.out.println("merges: " + numMerged);
            System.out.println("#pairs: " + (numPairs / (((System.currentTimeMillis() - stime)) / 1000d)) + "/s, #merged: " + (numMerged / (((System.currentTimeMillis() - stime)) / 1000d)) + "/s");
        }));

        GraphReader gr = new GraphReader();
        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.List());

        Graph pokec = gr.fromFile("resources/pokec.metis");

        gr.setReturnFormat(new GraphReader.Subgraph());
        gr.setInputFormat(new GraphReader.NodeList(pokec));

        ArrayList<InducedSubgraph> subgs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(new File("resources/communities.txt")))) {
            String line;
            while ((line = br.readLine()) != null)
                subgs.add((InducedSubgraph) gr.fromString(line));
        }

        InverseIndex index = new InverseIndex(pokec, subgs);

        final long stime = System.currentTimeMillis();

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                InducedSubgraph merged;
                InducedSubgraph a;
                InducedSubgraph b;
                while (true) {
                    // pick a random node
                    int randIndex1 = (int) Math.floor(Math.random() * index.size());

                    // pick two random communities having that node
                    ArrayList<InducedSubgraph> overlappingSubgraphs = index.getGraphsHaving(randIndex1); //TODO READ LOCK THIS LIST... NEED PRODUCER/CONSUMER KIND OF THING
                    int randIndex2 = (int) Math.floor(Math.random() * overlappingSubgraphs.size());
                    int randIndex3 = (int) Math.floor(Math.random() * overlappingSubgraphs.size());

                    if (randIndex2 == randIndex3)
                        continue;

                    a = overlappingSubgraphs.get(randIndex2);
                    b = overlappingSubgraphs.get(randIndex3);
                    if (!a.getLock().tryLock())
                        continue;
                    if (!b.getLock().tryLock()) {
                        a.getLock().unlock();
                        continue;
                    }

                    numPairs++;

                    if (a.getNodeOverlapPercent(b) > nodeOverlapThreshold)
                        if (a.getEdgeOverlapPercent(b) > edgeOverlapThreshold) {
                            merged = a.merge(b);
                            double ev = merged.getEigenvalue();
                            if (ev > a.getEigenvalue() + evDeltaThreshold && ev > b.getEigenvalue() + evDeltaThreshold)
                                numMerged++;
                        }

                    a.getLock().unlock();
                    b.getLock().unlock();
                }
            });
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
    }
}
