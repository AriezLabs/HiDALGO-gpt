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

    public static void main(String[] args) throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.printf("checked: %d%n", nchecked);
            System.out.printf("removed: %d%n", nremoved);
            long time = (System.currentTimeMillis() - starttime) / 1000;
            System.out.printf("time: %ds%n", time);
            System.out.printf("throughput: %f/s%n", 1d * nchecked / time);
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
     * @param limit number of communities to process
     */
    public static void filterNodes(Graph main, double degreeCutoff, double numNodesCutoff, double numEdgesCutoff, int limit) throws IOException {
        GraphReader gw = new GraphReader();
        String id = String.format("-deg-%.2f-node-%.2f-edge-%.2f", degreeCutoff, numNodesCutoff, numEdgesCutoff);
        try(BufferedReader br = new BufferedReader(new FileReader(new File("resources/communities.txt")));
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("filter"+id+".txt")))) {
            gw.setInputFormat(new GraphReader.NodeList(main));
            gw.setReturnFormat(new GraphReader.Subgraph());
            String line;
            int i = 0;
            while((line = br.readLine()) != null && i++ < limit) {
                InducedSubgraph community = (InducedSubgraph) gw.fromString(line);
                Graph removed = community.removeStronglyConnectedNodes(degreeCutoff);
                // 1. if graph is no longer connected
                if(!removed.isConnected())
                    // 2. if graph has few strongly connected nodes
                    if (community.stronglyConnectedNodesCount(degreeCutoff) < community.getNodeCount() * numNodesCutoff)
                        // 3. if graph has few edges left after removal
                        if (removed.getUndirectedEdgeCount() < community.getUndirectedEdgeCount() * numEdgesCutoff) {
                            System.out.println(community);
                            nremoved++;
                            nchecked++;
                            continue;
                        }

                bw.write(community.getEigenvalue() + "\n");
                nchecked++;
            }
        }
    }

}
