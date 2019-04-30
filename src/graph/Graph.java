package graph;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.EigenDecomposition_F64;

/**
 * abstract base class, doesn't support adding nodes, adding edges is only supported for reading from files etc
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
     * @return second smallest eigenvalue of normalized laplacian of this graph
     */
    public double getEigenvalue() {
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

        double secondSmallest = Double.MAX_VALUE;
        for (int i = 0; i < evd.getNumberOfEigenvalues(); i++)
            if(evd.getEigenvalue(i).getReal() < secondSmallest && i != index)
                secondSmallest = evd.getEigenvalue(i).getReal();

        return secondSmallest;
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
    public int getUndirectedEdgeCount() { assert e % 2 == 0 : "uneven edge count"; return e / 2; }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
}
