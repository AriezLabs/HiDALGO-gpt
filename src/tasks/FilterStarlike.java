package tasks;

import graph.Graph;
import graph.InducedSubgraph;
import io.GraphReader;

import java.io.*;

/**
 * experiments on dealing with starlike communities
 */
public class FilterStarlike {
    public static int nremoved;
    public static int nchecked;
    public static long starttime;

    public static void main(String[] args) throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("------------------------");
            System.out.printf("checked: %d%n", nchecked);
            System.out.printf("removed: %d (%f%%)%n", nremoved, 100d*nremoved/nchecked);
            long time = (System.currentTimeMillis() - starttime) / 1000;
            System.out.printf("time: %ds%n", time);
            System.out.printf("throughput: %f/s%n", 1000d * nchecked / (System.currentTimeMillis() - starttime));
        }));


        GraphReader gw = new GraphReader();
        gw.setInputFormat(new GraphReader.Metis());
        gw.setReturnFormat(new GraphReader.List());
        Graph g = gw.fromFile("resources/pokec.metis");

        for (double degreeCutoff = 0.4; degreeCutoff < 0.9; degreeCutoff += 0.1)
            for (double numNodesCutoff = 0.1; numNodesCutoff < 0.5; numNodesCutoff += 0.1)
                for (double ccSizeCutoff = 0.1; ccSizeCutoff < 0.5; ccSizeCutoff += 0.1)
                    for (double edgeCountCutoff = 0.025; edgeCountCutoff < 0.5; edgeCountCutoff *= 2)
                        filterNodes(g, degreeCutoff, numNodesCutoff, ccSizeCutoff, edgeCountCutoff, 300000);
    }

    /**
     * reads communities from communities.txt, writes eigenvalues of communities that still are connected after
     * removing nodes with degree > n*degreeCutoff to removedStats0.x.txt; ignores the rest
     * @param main graph to induce subgraphs from
     * @param degreeCutoff throw out nodes with degree larger than n*degreeCutoff
     * @param limit number of communities to process, -1 for no limit
     */
    public static void filterNodes(Graph main, double degreeCutoff, double numNodesCutoff, double ccSizeCutoff, double edgeCountCutoff, int limit) throws IOException {
        nchecked = 0;
        nremoved = 0;
        starttime = System.currentTimeMillis();
        GraphReader gw = new GraphReader();
        String id = String.format("deg%.2f-node%.2f-cc-%.2f-edge-%.3f", degreeCutoff, numNodesCutoff, ccSizeCutoff, edgeCountCutoff);
        if(limit == -1)
            limit = Integer.MAX_VALUE;

        try(BufferedReader br = new BufferedReader(new FileReader(new File("resources/communities.txt")));
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(id+"-halfSizedCCs.txt")));
            BufferedWriter rmbw = new BufferedWriter(new FileWriter(new File(id+"-removed.txt")));
            BufferedWriter cpbw = new BufferedWriter(new FileWriter(new File(id+"-kept.txt")));
            BufferedWriter tsbw = new BufferedWriter(new FileWriter(new File(".txt")))) {

            gw.setInputFormat(new GraphReader.NodeList(main));
            gw.setReturnFormat(new GraphReader.Subgraph());

            String line;
            int i = 0;
            while((line = br.readLine()) != null && i++ < limit) {
                nchecked++;

                InducedSubgraph community = (InducedSubgraph) gw.fromString(line);
                Graph removed = community.removeStronglyConnectedNodes(degreeCutoff);

                // 1. if graph is no longer connected
                if(!removed.isConnected())
                    // 2. if graph has few strongly connected nodes
                    if (community.stronglyConnectedNodesCount(degreeCutoff) < community.getNodeCount() * numNodesCutoff)
                        // 3. if largest cc is < original size * constant
                        if (removed.getLargestCcSize() < community.getNodeCount() * ccSizeCutoff)
                            // 4. if number of remaining edges is low
                            if (removed.getEdgeCount() < community.getEdgeCount() * edgeCountCutoff) {
                                nremoved++;
                                rmbw.write(community.toString());
                                continue;
                            }

                cpbw.write(community.toString());

                if (Math.abs(1d * removed.getLargestCcSize() / community.getNodeCount() - 0.5) < 0.1)
                    bw.write(community + "\n--------------------\n");
            }
        }
        System.out.println("------------");
        System.out.println("strongly connected degree cutoff: " + degreeCutoff);
        System.out.println("number of strongly connected nodes cutoff: " + numNodesCutoff);
        System.out.println("size of largest cc cutoff: " + ccSizeCutoff);
        System.out.println("count of remaining edges cutoff: " + edgeCountCutoff);
        System.out.printf("checked: %d%n", nchecked);
        System.out.printf("removed: %d (%f%%)%n", nremoved, 100d*nremoved/nchecked);
        long time = (System.currentTimeMillis() - starttime) / 1000;
        System.out.printf("time: %ds%n", time);
        System.out.printf("throughput: %f/s%n", 1000d * nchecked / (System.currentTimeMillis() - starttime));
    }

}
