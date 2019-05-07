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

        starttime = System.currentTimeMillis();
            filterNodes(g, 0.74, 0.2, 0.4,10000);
    }

    /**
     * reads communities from communities.txt, writes eigenvalues of communities that still are connected after
     * removing nodes with degree > n*degreeCutoff to removedStats0.x.txt; ignores the rest
     * @param main graph to induce subgraphs from
     * @param degreeCutoff throw out nodes with degree larger than n*degreeCutoff
     * @param limit number of communities to process, -1 for no limit
     */
    public static void filterNodes(Graph main, double degreeCutoff, double numNodesCutoff, double ccSizeCutoff, int limit) throws IOException {
        GraphReader gw = new GraphReader();
        String id = String.format("deg%.2f-node%.2f-cc-%.2f", degreeCutoff, numNodesCutoff, ccSizeCutoff);
        if(limit == -1)
            limit = Integer.MAX_VALUE;

        try(BufferedReader br = new BufferedReader(new FileReader(new File("resources/communities.txt")));
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(id+".txt")));
            BufferedWriter tsbw = new BufferedWriter(new FileWriter(new File("testset.txt")))) {

            gw.setInputFormat(new GraphReader.NodeList(main));
            gw.setReturnFormat(new GraphReader.Subgraph());

            String line;
            int i = 0;
            while((line = br.readLine()) != null && i++ < limit) {
                nchecked++;

                InducedSubgraph community = (InducedSubgraph) gw.fromString(line);
                Graph removed = community.removeStronglyConnectedNodes(degreeCutoff);
                double ev = community.getEigenvalue();
                if(Math.abs(ev - 1) < 0.01)
                    System.out.println(community);
                //tsbw.write(ev + "\n");

                // 1. if graph is no longer connected
                if(!removed.isConnected())
                    // 2. if graph has few strongly connected nodes
                    if (community.stronglyConnectedNodesCount(degreeCutoff) < community.getNodeCount() * numNodesCutoff)
                        if(removed.getLargestCcSize() < community.getNodeCount() * 0.66)
                        // 3. if largest cc is < original size * constant
                        if (removed.getUndirectedEdgeCount() < community.getUndirectedEdgeCount() * ccSizeCutoff) {
                            nremoved++;
                            continue;
                        }

                //bw.write(community.getEigenvalue() + "\n");
            }
        }
    }

}
