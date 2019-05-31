package graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Decorator: Lookups are passed on to parent graph, lightweight to create
 */
public class InducedSubgraph extends Graph {
    private HashMap<Integer, Integer> mapToOriginalIDs;
    private HashMap<Integer, Integer> mapToNewIDs;
    private Graph g;
    private ReentrantLock lock = new ReentrantLock();

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
            // problem: creation of partially initialized object if we get to this point
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
        if(node > n || node < 0)
            throw new IllegalArgumentException("node id out of range for this subgraph");
        ArrayList<Integer> neighbors = new ArrayList<>();
        int neighbor;
        for (int i : g.getNeighbors(mapToOriginalIDs.get(node))) {
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
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                if (hasEdge(i, j))
                    mat[i][j] = 1;
        return mat;
    }

    /**
     * @return count of edges (undirected edges counted twice)
     */
    @Override
    public int getEdgeCount() {
        int count = 0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                if (hasEdge(i, j))
                    count++;
        return count;
    }

    /**
     * @return ID of node in parent graph, -1 if node is not in this graph
     */
    public int getOriginalNodeID(int node) {
        return mapToOriginalIDs.getOrDefault(node, -1);
    }

    /**
     * @return ID of node in this graph, -1 if node is not in this graph
     */
    public int getNewNodeID(int original) {
        return mapToNewIDs.getOrDefault(original, -1);
    }

    /**
     * @return list of all original node ids of this graph
     */
    public ArrayList<Integer> toNodeList() {
        ArrayList<Integer> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++)
            nodes.add(getOriginalNodeID(i));
        return nodes;
    }

    /**
     * @return number of edges incident to non-overlapping parts of this subgraph and the other subgraph
     *         divided by number of edges in non-overlapping part of the larger of this and other
     */
    public double getEdgeOverlapPercent(InducedSubgraph other) {
        if (other.g != this.g)
            return 0;

        if (other.n < this.n)
            return other.getEdgeOverlapPercent(this);

        // assert: this.n <= other.n

        ArrayList<Integer> nonoverlappingNodesOfThis = getNonoverlappingNodes(other);
        ArrayList<Integer> nonoverlappingNodesOfOther = other.getNonoverlappingNodes(this);

        if (nonoverlappingNodesOfThis.size() <= 0) { // overlapping communities...
            return Double.MAX_VALUE;
        }

        //assert nonoverlappingNodesOfThis.size() > 0 : "found embedded community";

        int incidentEdges = 0;
        for (int nodeA : nonoverlappingNodesOfThis)
            for (int nodeB : nonoverlappingNodesOfOther)
                if (g.hasEdge(nodeA, nodeB))
                    incidentEdges++;

        int nonoverlappingEdges = new InducedSubgraph(g, nonoverlappingNodesOfThis).getUndirectedEdgeCount();

        return (double) incidentEdges / nonoverlappingEdges;
    }

    public boolean hasOriginalNode(int originalNodeId) {
        return mapToOriginalIDs.containsValue(originalNodeId);
    }

    public InducedSubgraph merge(InducedSubgraph other) {
        assert other.g == this.g : "cannot merge induced subgraphs stemming from different main graphs";

        ArrayList<Integer> nodesOfThis = toNodeList();
        ArrayList<Integer> nodesOfOther = other.toNodeList();
        ArrayList<Integer> merged = new ArrayList<>();

        merged.addAll(nodesOfThis);

        for (int node : nodesOfOther)
            if (!merged.contains(node))
                merged.add(node);

        return new InducedSubgraph(g, merged);
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
