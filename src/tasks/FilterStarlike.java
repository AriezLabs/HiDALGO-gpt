package tasks;

import graph.Graph;
import graph.InducedSubgraph;
import io.GraphReader;

import java.io.*;
import java.util.ArrayList;

/**
 * experiments on dealing with starlike communities
 */
public class FilterStarlike {
    public static int nremoved;
    public static int nchecked;
    public static long starttime;
    public static boolean calculateEVs;

    public static void main(String[] args) throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("------------------------");
            System.out.printf("checked: %d%n", nchecked);
            System.out.printf("removed: %d (%f%%)%n", nremoved, 100d*nremoved/nchecked);
            long time = (System.currentTimeMillis() - starttime) / 1000;
            System.out.printf("time: %ds%n", time);
            System.out.printf("throughput: %f/s%n", 1000d * nchecked / (System.currentTimeMillis() - starttime));
        }));

        nchecked = 0;
        nremoved = 0;

        GraphReader gw = new GraphReader();
        gw.setInputFormat(new GraphReader.Metis());
        gw.setReturnFormat(new GraphReader.List());
        Graph g = gw.fromFile("resources/pokec.metis");

        calculateEVs = false;

        ArrayList<InducedSubgraph> graphs = new ArrayList<>();
        gw.setInputFormat(new GraphReader.NodeList(g));
        gw.setReturnFormat(new GraphReader.Subgraph());

        String line;
        int i = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(new File("resources/communities.txt")))) {
            while ((line = br.readLine()) != null && i++ < 30000) {
                graphs.add((InducedSubgraph) gw.fromString(line));
            }
        }

        ArrayList<Thread> threads = new ArrayList<>();

        for (double degreeCutoff = 0.6; degreeCutoff < 0.8; degreeCutoff += 0.1)
            for (double ccSizeCutoff = 0.5; ccSizeCutoff < 1; ccSizeCutoff += 0.1) {
                double finalDegreeCutoff = 0.7;
                double finalNumNodesCutoff = 0.2;
                double finalCcSizeCutoff = ccSizeCutoff;
                double finalEdgeCountCutoff = 0.2;
                threads.add(new Thread(() -> {
                    try {
                        filterNodes(finalDegreeCutoff, finalNumNodesCutoff, finalCcSizeCutoff, finalEdgeCountCutoff, graphs);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }));
            }

        starttime = System.currentTimeMillis();
        for (Thread t : threads)
            t.start();

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * reads communities from communities.txt, writes eigenvalues of communities that still are connected after
     * removing nodes with degree > n*degreeCutoff to removedStats0.x.txt; ignores the rest
     * @param degreeCutoff throw out nodes with degree larger than n*degreeCutoff
     */
    public static void filterNodes(double degreeCutoff, double numNodesCutoff, double ccSizeCutoff, double edgeCountCutoff, Iterable<InducedSubgraph> graphSupply) throws IOException {
        String id = String.format("deg%.2f-node%.2f-cc-%.2f-edge-%.3f", degreeCutoff, numNodesCutoff, ccSizeCutoff, edgeCountCutoff);

        try(
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(id+"-relativeToOld-halfSizedCCs.txt")));
            BufferedWriter rmbw = new BufferedWriter(new FileWriter(new File(id+"-relativeToOld-removed.txt")));
            BufferedWriter cpbw = new BufferedWriter(new FileWriter(new File(id+"-relativeToOld-kept.txt")));
            BufferedWriter evbw = new BufferedWriter(new FileWriter(new File(id+"-relativeToOld-evs.txt")));
            BufferedWriter tsbw = new BufferedWriter(new FileWriter(new File("testset.txt")))) {

            for (InducedSubgraph community : graphSupply) {
                nchecked++;

                Graph removed = community.removeStronglyConnectedNodes(degreeCutoff);

                if (calculateEVs)
                    tsbw.write(community.getEigenvalue() + "\n");

                // 1. if graph is no longer connected
                if (!removed.isConnected())
                    // 2. if graph has few strongly connected nodes
                    if (community.stronglyConnectedNodesCount(degreeCutoff) < community.getNodeCount() * numNodesCutoff)
                        // 3. if largest cc is < original size * constant
                        if (removed.getLargestCcSize() < removed.getNodeCount() * ccSizeCutoff)
                            // 4. if number of remaining edges is low
                            if (removed.getEdgeCount() < community.getEdgeCount() * edgeCountCutoff) {
                                nremoved++;
                                rmbw.write(community.toString() + "\n");
                                continue;
                            }

                cpbw.write(community.toString() + "\n");
                evbw.write(community.getEigenvalue() + "\n");

                if (Math.abs(1d * removed.getLargestCcSize() / community.getNodeCount() - 0.5) < 0.1)
                    bw.write(community + "\n");
            }
        }
    }
}
