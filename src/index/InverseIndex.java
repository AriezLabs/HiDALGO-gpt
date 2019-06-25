package index;

import graph.Graph;
import graph.InducedSubgraph;

import java.util.ArrayList;
import java.util.Collections;

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

        for (ArrayList<InducedSubgraph> al : index)
            Collections.sort(al);
    }

    /**
     * find two unlocked overlapping subgraphs, lock them,
     * @return new MergeCandidate with two random overlapping subgraphs
     */
    public MergeCandidate getCandidate() {
        synchronized (this) {
            while (true) {
                ArrayList<InducedSubgraph> overlappingSubgraphs = index[(int) Math.floor(Math.random() * index.length)];

                if(overlappingSubgraphs.size() == 0)
                    continue;

                int randIndex1 = (int) Math.floor(Math.random() * overlappingSubgraphs.size());

                InducedSubgraph a = overlappingSubgraphs.get(randIndex1);

                if (!a.getLock().tryLock()) {
                    continue;
                }

                return new MergeCandidate(a, overlappingSubgraphs);
            }
        }
    }

    /**
     * remove pair from index, add merged graph in their places, sustain sorting
     * @param pair
     */
    public void update(MergeCandidate pair) {
        synchronized (this) {
            for (int i = 0; i < pair.a.getNodeCount(); i++)
                synchronized (index[pair.a.getOriginalNodeID(i)]) {
                    index[pair.a.getOriginalNodeID(i)].remove(pair.a);
                }
            for (int i = 0; i < pair.b.getNodeCount(); i++)
                synchronized (index[pair.b.getOriginalNodeID(i)]) {
                    index[pair.b.getOriginalNodeID(i)].remove(pair.b);
                }
            for (int i = 0; i < pair.merged.getNodeCount(); i++) {
                int originalId = pair.merged.getOriginalNodeID(i);
                synchronized (index[originalId]) {
                    int insertionIndex = 0;
                    while (insertionIndex < index[originalId].size() && pair.merged.compareTo(index[originalId].get(insertionIndex)) >= 0)
                        insertionIndex++;
                    index[originalId].add(insertionIndex, pair.merged);
                }
            }

            source.remove(pair.a);
            source.remove(pair.b);
            source.add(pair.merged);
        }
    }
}
