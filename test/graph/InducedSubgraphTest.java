package graph;

import io.GraphReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InducedSubgraphTest {
    InducedSubgraph g;
    Graph main;

    @BeforeEach
    void reset() throws IOException {
        GraphReader gr = new GraphReader();
        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.Matrix());
        main = gr.fromFile("testResources/medium.metis");
        gr.setInputFormat(new GraphReader.NodeList(main));
        gr.setReturnFormat(new GraphReader.Subgraph());
        g = (InducedSubgraph) gr.fromFile("testResources/medium.nl");
    }

    @Test
    void getNeighbors() {
        ArrayList<Integer> neighbors = g.getNeighbors(g.getNewNodeID(13));
        assertEquals(3, neighbors.size());
        assertTrue(neighbors.contains(g.getNewNodeID(0)));
        assertTrue(neighbors.contains(g.getNewNodeID(3)));
        assertTrue(neighbors.contains(g.getNewNodeID(10)));
    }

    @Test
    void hasEdge() {
    }

    @Test
    void toMatrix() {
    }

    @Test
    void getEdgeCount() {
    }

    @Test
    void getOriginalNodeID() {
    }

    @Test
    void getEdgeOverlapPercent() throws IOException {
        GraphReader gr = new GraphReader();
        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.Matrix());
        Graph main = gr.fromFile(new File("testResources/overlapBase.metis"));

        gr.setInputFormat(new GraphReader.NodeList(main));
        gr.setReturnFormat(new GraphReader.Subgraph());

        InducedSubgraph sub1 = (InducedSubgraph) gr.fromFile(new File("testResources/overlap1.nl"));
        InducedSubgraph sub2 = (InducedSubgraph) gr.fromFile(new File("testResources/overlap2.nl"));

        System.out.println(sub1.getEdgeOverlapPercent(sub2));
    }

    @Test
    void testMerge() throws IOException {
        GraphReader gr = new GraphReader();
        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.Matrix());
        Graph main = gr.fromFile(new File("testResources/medium.metis"));

        gr.setInputFormat(new GraphReader.NodeList(main));
        gr.setReturnFormat(new GraphReader.Subgraph());
        InducedSubgraph sg1 = (InducedSubgraph) gr.fromFile("testResources/medium.nl");
        InducedSubgraph sg2 = (InducedSubgraph) gr.fromFile("testResources/medium.nl.2");

        InducedSubgraph merged = sg1.merge(sg2);
        int[] nodes = {0, 1, 2, 3, 10, 13, 16};
        ArrayList<Integer> whyisthissocomplicated = new ArrayList<>();
        for(int i : nodes) {
            assertTrue(merged.toNodeList().contains(i));
            whyisthissocomplicated.add(i);
        }

        InducedSubgraph test = new InducedSubgraph(main, whyisthissocomplicated);
        assertEquals(test.getEigenvalue(), merged.getEigenvalue(), 0.0000001);
    }
}