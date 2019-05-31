package index;

import graph.Graph;
import graph.InducedSubgraph;

import java.util.ArrayList;

public class InverseIndex {
    private ArrayList<InducedSubgraph>[] index;
    private ArrayList<InducedSubgraph> source;

    /**
     * construct an inverse index mapping a node id to arraylist of subgraphs containing that node
     */
    public InverseIndex(Graph main, ArrayList<InducedSubgraph> subgraphs) {
        index = new ArrayList[main.getNodeCount()];
        source = subgraphs;

        for (int i = 0; i < index.length; i++)
            index[i] = new ArrayList<>();

        for (InducedSubgraph s : subgraphs)
            for (int node : s.toNodeList())
                index[node].add(s);
    }

    public ArrayList<InducedSubgraph> getGraphsHaving(int node) {
        return index[node];
    }

    /**
     * find two unlocked overlapping subgraphs, lock them,
     * @return new OverlappingPair with two random overlapping subgraphs
     */
    public OverlappingPair getRandomPair() {
        while (true) {
            ArrayList<InducedSubgraph> overlappingSubgraphs = index[(int) Math.floor(Math.random() * index.length)];

            int randIndex1 = (int) Math.floor(Math.random() * overlappingSubgraphs.size());
            int randIndex2 = (int) Math.floor(Math.random() * overlappingSubgraphs.size());

            if(randIndex1 == randIndex2)
                continue;

            InducedSubgraph a = overlappingSubgraphs.get(randIndex1);
            InducedSubgraph b = overlappingSubgraphs.get(randIndex2);

            if(!a.getLock().tryLock()) {
                continue;
            }
            if(!b.getLock().tryLock()) {
                a.getLock().unlock();
                continue;
            }

            return new OverlappingPair(a, b);
        }
    }

    /**
     * remove pair from index, add merged graph in their places
     * @param pair
     */
    public void update(OverlappingPair pair) {
        for (int i = 0; i < pair.a.getNodeCount(); i++)
            index[pair.a.getOriginalNodeID(i)].remove(pair.a);
        for (int i = 0; i < pair.b.getNodeCount(); i++)
            index[pair.b.getOriginalNodeID(i)].remove(pair.b);
        for (int i = 0; i < pair.merged.getNodeCount(); i++)
            index[pair.merged.getOriginalNodeID(i)].add(pair.merged);

        source.remove(pair.a);
        source.remove(pair.b);
        source.add(pair.merged);
    }
}
