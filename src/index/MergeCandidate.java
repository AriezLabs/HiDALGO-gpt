package index;

import graph.InducedSubgraph;
import tasks.MergeOverlappingCommunities;

import java.util.ArrayList;

public class MergeCandidate {
    public InducedSubgraph a;
    public InducedSubgraph b;
    public InducedSubgraph merged;

    private final ArrayList<InducedSubgraph> source;
    private int currentIndex;

    // a is already locked
    public MergeCandidate(InducedSubgraph a, ArrayList<InducedSubgraph> source) {
        currentIndex = 0;
        this.a = a;
        this.source = source;
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

    /**
     * find next suitable graph, that is, not a and not locked; locks graph, unlocks previous graph
     * @return true if suitable next was found, false if end of list has been reached
     */
    public boolean next() {
        synchronized (source) {
            while (++currentIndex < source.size())
                if (source.get(currentIndex) != a && source.get(currentIndex).getLock().tryLock()) {
                    if (b != null)
                        b.getLock().unlock();
                    b = source.get(currentIndex);
                    merged = null;
                    return true;
                }
            return false;
        }
    }

    public double getMergedEv() {
        if (merged == null)
            createMerged();
        return merged.getEigenvalue();
    }

    public double getDelta() {
        switch (MergeOverlappingCommunities.evCompareStrategy) {
            case 1: // delta := 2 * merged - a - b
                return 2 * getMergedEv() - a.getEigenvalue() - b.getEigenvalue();
            default: // delta := merged - larger of both
                return getMergedEv() - (a.getNodeCount() > b.getNodeCount() ? a.getEigenvalue() : b.getEigenvalue());

        }
    }

    public void unlock() {
        assert a.getLock().isLocked() : "attempt to unlock unlocked lock";
        a.getLock().unlock();

        if (b != null) {
            assert b.getLock().isLocked() : "attempt to unlock unlocked lock";
            b.getLock().unlock();
        }
    }

}
