package graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Decorator: Lookups are passed on to parent graph, lightweight to create
 */
public class InducedSubgraph extends Graph {
    private HashMap<Integer, Integer> mapToOriginalIDs;
    private HashMap<Integer, Integer> mapToNewIDs;
    private Graph g;

    /**
     * create new subgraph from graph and list of nodes
     */
    public InducedSubgraph(Graph g, ArrayList<Integer> nodes) {
        this(g, nodes.toArray(new Integer[0]));
    }

    public InducedSubgraph(Graph g, Integer[] nodes) {
        super(nodes.length);

        // check if nodes in range
        int max = Arrays.stream(nodes).max(Integer::compareTo).orElse(-1);
        if(max >= g.getNodeCount())
            // problem: partially initialized object...
            throw new IllegalArgumentException(String.format("cannot create induced subgraph: graph %s does not have node with id %d", g.name, max));

        this.g = g;

        mapToOriginalIDs = new HashMap<>();
        mapToNewIDs = new HashMap<>();
        for (int i = 0; i < nodes.length; i++) {
            mapToOriginalIDs.put(i, nodes[i]);
            mapToNewIDs.put(nodes[i], i);
        }
    }

    /**
     * @param node node to get neighbors from
     * @return neighbors that are in the subgraph
     */
    @Override
    public ArrayList<Integer> getNeighbors(int node) {
        ArrayList<Integer> neighbors = new ArrayList<>();
        int neighbor;
        for (int i : g.getNeighbors(node)) {
            neighbor = mapToNewIDs.getOrDefault(i, -1);
            if (neighbor != -1)
                neighbors.add(neighbor);
        }
        return neighbors;
    }

    @Override
    public boolean addEdge(int nodeFrom, int nodeTo) {
        throw new UnsupportedOperationException("cannot add edge to induced subgraph");
    }

    @Override
    public boolean hasEdge(int nodeFrom, int nodeTo) {
        if (hasNode(nodeFrom) && hasNode(nodeTo))
            return g.hasEdge(mapToOriginalIDs.get(nodeFrom), mapToOriginalIDs.get(nodeTo));
        else
            throw new IllegalArgumentException(String.format("cannot add edge: %d-node graph %s does not contain node %d or %d", getNodeCount(), getName(), nodeFrom, nodeTo));
    }

    /**
     * @return adjacency matrix of this subgraph
     */
    @Override
    public double[][] toMatrix() {
        double[][] mat = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j : getNeighbors(i))
                mat[i][j] = 1;
        }
        return mat;
    }

    /**
     * @return ID of node in parent graph, -1 if node is not in this graph
     */
    public int getOriginalNodeID(int node) {
        return mapToOriginalIDs.getOrDefault(node, -1);
    }
}
