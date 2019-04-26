package graph;

import java.util.ArrayList;

/**
 * candidate for concurrent graph data structure
 */
public class AdjacencyList extends Graph {
    private ArrayList<Integer>[] al;

    /**
     * create empty graph with zero nodes
     */
    public AdjacencyList(int n) {
        super(n);
        al = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            al[i] = new ArrayList<>();
        }
    }

    /**
     * create new adjacency lists copying lists from parameter
     * unsafe; no integrity checks done
     */
    public AdjacencyList(ArrayList<Integer>[] lists) {
        super(lists.length);
        this.al = lists;
    }

    @Override
    public ArrayList<Integer> getNeighbors(int node) {
        return al[node];
    }

    @Override
    public boolean addEdge(int nodeFrom, int nodeTo) {
        if(nodeFrom < 0 || nodeFrom >= n)
            throw new IllegalArgumentException(String.format("cannot add edge: nodeFrom out of bounds (%d)", nodeFrom));
        if(nodeTo < 0 || nodeTo >= n)
            throw new IllegalArgumentException(String.format("cannot add edge: nodeTo out of bounds (%d)", nodeTo));

        if(al[nodeFrom].contains(nodeTo))
            return false;
        else
            al[nodeFrom].add(nodeTo);
        e++;
        return true;
    }

    @Override
    public boolean hasEdge(int nodeFrom, int nodeTo) {
        if(hasNode(nodeFrom) && hasNode(nodeTo))
            return al[nodeFrom].contains(nodeTo);
        else
            throw new IllegalArgumentException(String.format("cannot add edge: %d-node graph %s does not contain node %d or %d", getNodeCount(), getName(), nodeFrom, nodeTo));
    }

    @Override
    public double[][] toMatrix() {
        double[][] mat = new double[n][n];
        for (int i = 0; i < n; i++)
            for(int j : al[i])
                mat[i][j] = 1;
        return mat;
    }
}
