package graph;

/**
 * abstract base class, doesn't support adding nodes, adding edges is only supported for reading incrementally
 * pseudo-immutable
 */
public abstract class Graph {
    int n;
    int e;
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

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }

}
