package tasks;

import graph.Graph;
import graph.InducedSubgraph;

import java.util.ArrayList;

/**
 * experiments on dealing with starlike communities
 */
public class FilterStarlike {
    public static void main(String[] args) {
        /*
        for each community:
            print ev
            if g is no longer connected after removing nodes:
                append "dead" (or mark in some way)
         */
    }

    /**
     *
     * @param g graph to work with
     * @param cutoff if a node has more than n * cutoff nodes, remove it (e.g. n/2)
     * @return InducedSubgraph with nodes removed accordingly
     */
    InducedSubgraph removeStronglyConnectedNodes(InducedSubgraph g, double cutoff) {
        // filter out nodes with enough neighbors, save into filteredNodes
        ArrayList<Integer> filteredNodes = new ArrayList<>();
        for (int i = 0; i < g.getNodeCount(); i++) {
            if (g.getDegree(i) < g.getUndirectedEdgeCount() * cutoff)
                filteredNodes.add(i);
        }
        
        if(filteredNodes.size() == g.getNodeCount())
            return g;
        else
            return new InducedSubgraph(g, filteredNodes);
    }
}
