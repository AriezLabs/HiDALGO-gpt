package graph;

import io.GraphWriter;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.EigenDecomposition_F64;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.stream.IntStream;

/**
 * abstract base class, doesn't support adding nodes, adding edges is only supported for reading from files etc
 * pseudo-immutable
 */
public abstract class Graph implements Comparable<Graph> {
    int n;
    int e;
    double ev = -1;
    String name;

    Graph(int n) {
        this.n = n;
        this.name = "defaultName";
    }

    /**
     * @param node node to get neighbors from
     * @return Iterable containing all neighbors of node
     */
    public abstract Iterable<Integer> getNeighbors(int node);

    /**
     * implemented via getNeighbors
     * @return degree of node
     */
    public int getDegree(int node) {
        int deg = 0;
        for (int ignored : getNeighbors(node))
            deg++;
        return deg;
    }

    /**
     * breadth first traversal of the neighbors of a node
     * @param start node to start out with
     * @return list of nodes in order of visit
     */
    private ArrayList<Integer> BFTraverse(int start) {
        if (!hasNode(start))
            throw new IllegalArgumentException(String.format("cannot traverse %d-node graph starting from node %d", n, start));

        final short WHITE = 0;
        final short GRAY  = 1;
        final short BLACK = 2;

        ArrayList<Integer> nodes = new ArrayList<>();
        LinkedList<Integer> queue = new LinkedList<>();
        short[] visited = new short[n];
        queue.push(start);

        while (!queue.isEmpty()) {
            int node = queue.poll();
            visited[node] = BLACK;
            nodes.add(node);
            for(Integer neighbor : getNeighbors(node))
                if(visited[neighbor] == WHITE) {
                    queue.push(neighbor);
                    visited[neighbor] = GRAY;
                }
        }

        return nodes;
    }

    /**
     * @return second smallest eigenvalue of normalized laplacian of this graph
     */
    public double getEigenvalue() {
        if (ev != -1)
            return ev;

        double[][] matrix = toMatrix();

        // get laplacian
        DMatrixRMaj laplacian = new DMatrixRMaj(matrix.length, matrix.length);
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                double newValue = 0;
                if(i == j && getDegree(i) != 0)
                    newValue = 1;
                else if (hasEdge(i, j))
                    newValue = -1 / Math.sqrt(getDegree(i) * getDegree(j));
                laplacian.set(i, j, newValue);
            }
        }

        // calculate eigenvalues
        EigenDecomposition_F64<DMatrixRMaj> evd = DecompositionFactory_DDRM.eig(laplacian.getNumElements(), false);
        evd.decompose(laplacian);

        // find minimum index
        double min = Double.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < evd.getNumberOfEigenvalues(); i++)
            if(evd.getEigenvalue(i).getReal() < min) {
                min = evd.getEigenvalue(i).getReal();
                index = i;
            }

        // find second smallest
        double secondSmallest = Double.MAX_VALUE;
        for (int i = 0; i < evd.getNumberOfEigenvalues(); i++)
            if(evd.getEigenvalue(i).getReal() < secondSmallest && i != index)
                secondSmallest = evd.getEigenvalue(i).getReal();

        ev = secondSmallest;
        return secondSmallest;
    }

    public void setEigenvalue(double ev) {
        assert this.ev == -1 : "ev already known, cannot set";
        this.ev = ev;
    }

    /**
     * @return true iff graph is fully connected or empty
     */
    public boolean isConnected() {
        return n == 0 || BFTraverse(0).size() == n;
    }

    /**
     * @return sizes of all connected components, empty arraylist if graph is empty
     */
    public ArrayList<Integer> getCcSizes() {
        ArrayList<Integer> sizes = new ArrayList<>();
        ArrayList<Integer> traversed = new ArrayList<>();

        int start = 0;
        while (traversed.size() < n && start != -1) {
            ArrayList<Integer> component = BFTraverse(start);
            traversed.addAll(component);
            sizes.add(component.size());
            start = IntStream.range(0, n).filter(n -> !traversed.contains(n)).findFirst().orElse(-1);
        }

        return sizes;
    }

    public int getLargestCcSize() {
        return getCcSizes().stream().max(Integer::compareTo).orElse(0);
    }

    public int getNumberOfCcs() {
        return getCcSizes().size();
    }

    /**
     * @return false if edge already present, else true
     */
    public abstract boolean addEdge(int nodeFrom, int nodeTo);
    public abstract boolean hasEdge(int nodeFrom, int nodeTo);

    /**
     * @return adjacency matrix of graph
     */
    public abstract double[][] toMatrix();

    public boolean hasNode(int node) { return node < n && node >= 0; }

    public int getNodeCount() { return n; }
    public int getEdgeCount() { return e; }
    public int getUndirectedEdgeCount() {
        int e = getEdgeCount();
        assert e % 2 == 0 : "uneven edge count";
        return e / 2;
    }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
    public String toString() {
        GraphWriter gw = new GraphWriter();
        gw.setFormat(new GraphWriter.Metis());
        try {
            return gw.toString(this);
        } catch (IOException e) {
            throw new RuntimeException("never gonna happen");
        }
    }

    /**
     * returns number of nodes with degree larger than n*cutoff
     */
    public int stronglyConnectedNodesCount(double cutoff) {
        int ctr = 0;
        for (int i = 0; i < n; i++) {
            if(getDegree(i) > n*cutoff)
                ctr++;
        }
        return ctr;
    }

    /**
     * @return InducedSubgraph of this with nodes with degree larger than n*cutoff removed
     */
    public InducedSubgraph removeStronglyConnectedNodes(double cutoff) {
        ArrayList<Integer> filteredNodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (getDegree(i) <= n * cutoff) {
                filteredNodes.add(i);
            }
        }
        return new InducedSubgraph(this, filteredNodes);
    }

    /**
     * InducedSubgraph overrides to return node ids in original graph
     * @return list of all node ids of this graph
     */
    public ArrayList<Integer> toNodeList() {
        ArrayList<Integer> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++)
            nodes.add(i);
        return nodes;
    }

    /**
     * @param other other Graph to check against
     * @return percentage of nodes contained both in this and the other graph
     */
    public ArrayList<Integer> getOverlappingNodes(Graph other) {
        ArrayList<Integer> a = toNodeList();
        ArrayList<Integer> b = other.toNodeList();
        ArrayList<Integer> overlapping = new ArrayList<>();

        // naive, might be too slow
        for (int i = 0; i < n; i++)
            for (int j = 0; j < other.n; j++)
                if (b.get(j).equals(a.get(i))) {
                    overlapping.add(b.get(j));
                    break;
                }

        return overlapping;
    }

    /**
     * @return arraylist of nodes of this graph whose id is not also contained in the nodelist of other,
     *         in case of InducedSubgraph node ids are given in terms of node ids in original graph
     */
    public ArrayList<Integer> getNonoverlappingNodes(Graph other) {
        ArrayList<Integer> originalNodesOfThis = toNodeList();
        ArrayList<Integer> originalNodesOfOther = other.toNodeList();
        ArrayList<Integer> nonoverlappingNodes= new ArrayList<>();

        outer: for (int i : originalNodesOfThis) {
            for (int j : originalNodesOfOther)
                if (i == j)
                    continue outer;
            // if node i is not present in other graph, it's nonoverlapping
            nonoverlappingNodes.add(i);
        }

        return nonoverlappingNodes;
    }

    /**
     * @return percentage of nodes both in this graph and the other graph (by id)
     */
    public double getNodeOverlapPercent(Graph other) {
        return (double) getOverlappingNodes(other).size() / n;
    }

    @Override
    public int compareTo(Graph other) {
        return this.n - other.n;
    }
}
