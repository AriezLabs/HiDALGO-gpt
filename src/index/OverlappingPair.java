package index;

import graph.InducedSubgraph;

public class OverlappingPair {
    InducedSubgraph a;
    InducedSubgraph b;
    InducedSubgraph merged;

    public OverlappingPair(InducedSubgraph a, InducedSubgraph b) {
        this.a = a;
        this.b = b;
    }

    public boolean nodesOverlapping(double moreThan) {
        return a.getNodeOverlapPercent(b) >= moreThan;
    }

    public boolean edgesOverlapping(double moreThan) {
        return a.getEdgeOverlapPercent(b) >= moreThan;
    }

    public void createMerged() {
        merged = a.merge(b);
    }

    public double getMergedEv() {
        if (merged == null)
            createMerged();
        return merged.getEigenvalue();
    }

    public double getDelta() {
        return (getMergedEv() - a.getEigenvalue()) + (getMergedEv() - b.getEigenvalue()) / 2;
    }

    public void unlock() {
        assert a.getLock().isLocked() && b.getLock().isLocked() : "attempt to unlock unlocked lock";
        a.getLock().unlock();
        b.getLock().unlock();
    }
}
