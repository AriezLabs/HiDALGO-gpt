package graph;

import java.util.ArrayList;

public class AdjacencyMatrix extends Graph {
    private boolean[][] mat;

    /**
     * create new empty graph with n nodes and 0 edges
     */
    public AdjacencyMatrix(int n) {
        super(n);
        mat = new boolean[n][n];
    }

    public AdjacencyMatrix(double[][] matrix) {
        this(util.convert.toBoolean(matrix));
    }

    public AdjacencyMatrix(int[][] matrix) {
        this(util.convert.toBoolean(matrix));
    }

    public AdjacencyMatrix(boolean[][] matrix) {
        super(matrix.length);
        this.mat = matrix;
    }

    @Override
    public ArrayList<Integer> getNeighbors(int node) {
        ArrayList<Integer> l = new ArrayList<>(e/n);
        for(int i = 0; i < n; i++)
            if (mat[node][i])
                l.add(i);
        return l;
    }

    @Override
    public boolean addEdge(int nodeFrom, int nodeTo) {
        if(!hasNode(nodeFrom))
            throw new IllegalArgumentException(String.format("cannot add edge: nodeFrom out of bounds (%d)", nodeFrom));
        if(!hasNode(nodeTo))
            throw new IllegalArgumentException(String.format("cannot add edge: nodeTo out of bounds (%d)", nodeTo));

        if(mat[nodeFrom][nodeTo])
            return false;
        e++;
        return (mat[nodeFrom][nodeTo] = true);
    }

    @Override
    public boolean hasEdge(int nodeFrom, int nodeTo) {
        if(hasNode(nodeFrom) && hasNode(nodeTo))
            return mat[nodeFrom][nodeTo];
        else
            throw new IllegalArgumentException(String.format("cannot add edge: %d-node graph %s does not contain node %d or %d", getNodeCount(), getName(), nodeFrom, nodeTo));
    }

    @Override
    public double[][] toMatrix() {
        double[][] ret = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                if (mat[i][j])
                    ret[i][j] = 1;
        return ret;
    }
}
