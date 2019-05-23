package index;

import graph.Graph;
import graph.InducedSubgraph;

import java.util.ArrayList;

public class InverseIndex {
    private ArrayList<InducedSubgraph>[] index;

    /**
     * construct an inverse index mapping a node id to arraylist of subgraphs containing that node
     */
    public InverseIndex(Graph main, Iterable<InducedSubgraph> subgraphs) {
        index = new ArrayList[main.getNodeCount()];

        for (int i = 0; i < index.length; i++)
            index[i] = new ArrayList<>();

        for (InducedSubgraph s : subgraphs)
            for (int node : s.toNodeList())
                index[node].add(s);
    }

    public ArrayList<InducedSubgraph> getGraphsHaving(int node) {
        return index[node];
    }

    public int size() {
        return index.length;
    }
}
