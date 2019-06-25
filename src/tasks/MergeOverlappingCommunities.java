package tasks;

import graph.Graph;
import graph.InducedSubgraph;
import index.MergeCandidate;
import index.InverseIndex;
import io.GraphReader;
import io.GraphWriter;

import java.io.*;
import java.util.ArrayList;

public class MergeOverlappingCommunities {
    private static long stime;
    private static long numMerged = 0;
    private static double evImprovement = 0;
    private static long numPairs = 0;

    private static double edgeOverlapThreshold;
    private static double nodeOverlapThreshold;
    private static final double evDeltaThreshold = 0.01;

    private static int numThreads;
    private static int walltime;
    private static boolean walltimeExceeded = false;
    private static InverseIndex index;
    private static ArrayList<InducedSubgraph> subgs;

    public static int evCompareStrategy;
    public static double findBestMatchAmong;

    // number of communities to skip for each one read, for testing purposes (subtract 1)
    private static final int skip = 1;

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 6) {
            System.out.println("usage: MergeOverlappingCommunities numThreads walltimeSeconds edgeOverlapThreshold nodeOverlapThreshold evCompareStrategy findBestMatchAmong");
            System.out.println("evCompareStrategy: 0 (measure vs. larger) | 1 (measure vs. average)");
            System.out.println("findBestMatchAmong: [0, 1] (% of possible merging candidates to check before picking the best, but at least 1)");
            System.exit(1);
        } else {
            numThreads = Integer.parseInt(args[0]);
            walltime = Integer.parseInt(args[1]);
            edgeOverlapThreshold = Double.parseDouble(args[2]);
            nodeOverlapThreshold = Double.parseDouble(args[3]);
            evCompareStrategy = Integer.parseInt(args[4]);
            findBestMatchAmong = Integer.parseInt(args[5]);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));

        GraphReader gr = new GraphReader();
        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.List());

        Graph pokec = gr.fromFile("resources/pokec.metis");

        gr.setReturnFormat(new GraphReader.Subgraph());
        gr.setInputFormat(new GraphReader.NodeListWithEvs(pokec));

        subgs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(new File("resources/communitiesWithEvs.txt")))) {
            String line;
            if(skip != 1)
                System.err.println("WARNING: SKIPPING " + (skip-1) + " COMMUNITIES FOR EACH ONE READ");
            int i = 0;
            while ((line = br.readLine()) != null) {
                if (i++ % skip != 0)
                    continue;
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
                    MergeCandidate candidate = index.getCandidate();
                    numPairs++;

                    while(candidate.next()) { // TODO IMPLEMENT LOOKING FOR BEST MATCH AMONG N (LOCKING PROBLEM!)
                        if (candidate.nodesOverlapping(nodeOverlapThreshold)
                                && candidate.edgesOverlapping(edgeOverlapThreshold)
                                && candidate.getDelta() >= evDeltaThreshold) {
                            index.update(candidate);
                            evImprovement += candidate.getDelta();
                            numMerged++;
                            printMerge(candidate);
                            break;
                        }
                    }
                    candidate.unlock();
                }
            });
        }

        stime = System.currentTimeMillis();

        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }

        Thread.sleep(1000*walltime);
        System.out.println("exceeded walltime, stopping...");
        walltimeExceeded = true;

        for (int i = 0; i < threads.length; i++)
            threads[i].join();
    }

    /**
     * prints stats and saves state of communities, eigenvalues
     */
    private static class ShutdownHook implements Runnable {
        @Override
        public void run() {
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
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(String.format("./newEigenvalues-edge%f-node%f.txt", edgeOverlapThreshold, nodeOverlapThreshold))))) {
                for(InducedSubgraph subg : subgs)
                    bw.write(subg.getEigenvalue() +"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("writing new nodelists...");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(String.format("./newCommunities-edge%f-node%f.txt", edgeOverlapThreshold, nodeOverlapThreshold))))) {
                for(InducedSubgraph subg : subgs) {
                    for (int i : subg.toNodeList())
                        bw.write(i +" ");
                    bw.write("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("done.");
        }
    }

    private synchronized static void printMerge(MergeCandidate pair) {
        System.out.println("MERGING:");
        System.out.println("\t(" + pair.a.getEigenvalue() + ") " + pair.a.toNodeList());
        System.out.println("+\t(" + pair.b.getEigenvalue() + ") " + pair.b.toNodeList());
        System.out.println("=>\t(" + pair.merged.getEigenvalue() + ") " + pair.merged.toNodeList());
        GraphWriter gw = new GraphWriter();
        System.out.println(gw.toGraphViz(pair));
        System.out.println();
    }
}
