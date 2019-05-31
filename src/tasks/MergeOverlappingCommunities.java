package tasks;

import graph.Graph;
import graph.InducedSubgraph;
import index.InverseIndex;
import index.OverlappingPair;
import io.GraphReader;

import java.io.*;
import java.util.ArrayList;

public class MergeOverlappingCommunities {
    private static long stime;
    private static int numMerged = 0;
    private static double evImprovement = 0;
    private static int numPairs = 0;

    private static double edgeOverlapThreshold;
    private static double nodeOverlapThreshold;
    private static final double evDeltaThreshold = 0.01;

    private static int numThreads;
    private static int walltime;
    private static boolean walltimeExceeded = false;
    private static InverseIndex index;
    private static ArrayList<InducedSubgraph> subgs;

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 4) {
            System.out.println("usage: MergeOverlappingCommunities numThreads walltimeSeconds edgeOverlapThreshold nodeOverlapThreshold");
            System.out.println("writing new eigenvalues will take a while.. needs +1 hour or so of extra walltime");
            System.exit(1);
        } else {
            numThreads = Integer.parseInt(args[0]);
            walltime = Integer.parseInt(args[1]);
            edgeOverlapThreshold = Double.parseDouble(args[2]);
            nodeOverlapThreshold = Double.parseDouble(args[3]);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            float runtime = (System.currentTimeMillis() - stime) / 1000f;
            System.out.println("exiting...");
            System.out.println("time: " + runtime);
            System.out.println("#pairs: " + numPairs);
            System.out.println("pairs/s: " + (numPairs / (((System.currentTimeMillis() - stime)) / 1000d)));
            System.out.println("#merges: " + numMerged);
            System.out.println("merges/s: " + (numMerged / (((System.currentTimeMillis() - stime)) / 1000d)));
            System.out.println("avg ev improvement: " + (evImprovement / (2 * numMerged)));
            System.out.println("remaining items: " + subgs.size());

            System.out.println("writing new evs...");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File("./newEigenvalues.txt")))) {
                for(InducedSubgraph subg : subgs)
                    bw.write(subg.getEigenvalue() +"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("writing new nodelists...");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File("./newCommunities.txt")))) {
                for(InducedSubgraph subg : subgs) {
                    for (int i : subg.toNodeList())
                        bw.write(i +" ");
                    bw.write("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("done.");
        }));

        GraphReader gr = new GraphReader();
        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.List());

        Graph pokec = gr.fromFile("resources/pokec.metis");

        gr.setReturnFormat(new GraphReader.Subgraph());
        gr.setInputFormat(new GraphReader.NodeListWithEvs(pokec));

        subgs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(new File("resources/communitiesWithEvs.txt")))) {
            String line;
            while ((line = br.readLine()) != null) {
                InducedSubgraph g = (InducedSubgraph) gr.fromString(line);
                subgs.add(g);
            }
        }

        index = new InverseIndex(pokec, subgs);

        System.out.printf("indexed %d items%nstarting...%n", subgs.size());
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                while (!walltimeExceeded) {
                    OverlappingPair pair = index.getRandomPair();
                    numPairs++;

                    if (pair.nodesOverlapping(nodeOverlapThreshold)
                     && pair.edgesOverlapping(edgeOverlapThreshold)
                     && pair.getDelta() >= evDeltaThreshold) {
                        index.update(pair);
                        evImprovement += pair.getDelta();
                        numMerged++;

                    } else {
                        pair.unlock();
                    }
                }
            });
        }

        stime = System.currentTimeMillis();

        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }

        Thread.sleep(1000*walltime);
        //while((System.currentTimeMillis() - stime) < walltime * 1000)
        //    continue;
        System.out.println("exceeded walltime, stopping...");
        walltimeExceeded = true;

        for (int i = 0; i < threads.length; i++)
            threads[i].join();
    }
}
