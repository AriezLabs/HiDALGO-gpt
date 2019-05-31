package graph;

import io.GraphReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class GraphTest {
    Graph[] gs;
    ArrayList<Integer> neighborsOfZero;
    String medium = "testResources/medium.metis";
    String mediumNl = "testResources/medium.nl";
    String mediumNl2 = "testResources/medium.nl.2";

    @BeforeEach
    void reset() {
        int n = 6;
        neighborsOfZero = new ArrayList<>();
        neighborsOfZero.add(2);
        neighborsOfZero.add(3);
        neighborsOfZero.add(4);
        neighborsOfZero.add(5);
        gs = new Graph[2];
        gs[0] = new AdjacencyMatrix(n);
        gs[1] = new AdjacencyList(n);

        for(Graph g : gs)
            for(int i : neighborsOfZero)
                g.addEdge(0, i);
    }

    @Test
    void testNeighbors() {
        for(Graph g : gs) {
            int ctr = 0;
            for(int i : g.getNeighbors(0)) {
                assertTrue(neighborsOfZero.contains(i));
                ctr++;
            }
            assertEquals(4, ctr);
        }
    }

    @Test
    void testAdd() {
        for(Graph g : gs) {
            assertTrue(g.addEdge(1, 2));
            assertFalse(g.addEdge(1, 2));
            assertThrows(IllegalArgumentException.class, () -> g.addEdge(-1, 0));
            assertThrows(IllegalArgumentException.class, () -> g.addEdge(0, 99));
        }
    }

    @Test
    void testEv() throws IOException {
        GraphReader gr = new GraphReader();
        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.Matrix());
        Graph g = gr.fromFile("testResources/medium.metis");
        assertEquals(0.5781, g.getEigenvalue(), 0.0001);
        g = gr.fromFile("testResources/medium2.metis");
        assertEquals(0.4817, g.getEigenvalue(), 0.0001);
    }

    @Test
    void testConnected() throws IOException {
        GraphReader gr = new GraphReader();
        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.Matrix());
        Graph g = gr.fromFile("testResources/medium.metis");
        assertTrue(g.isConnected());
        g = gr.fromFile("testResources/medium2.metis");
        assertTrue(g.isConnected());
        g = gr.fromFile("testResources/disconnected.metis");
        assertFalse(g.isConnected());
    }

    @Test
    void getCcSizes() throws IOException {
        GraphReader gr = new GraphReader();
        gr.setReturnFormat(new GraphReader.List());
        gr.setInputFormat(new GraphReader.Metis());
        Graph g = gr.fromFile("testResources/multipleConnectedComponents.metis");
        ArrayList<Integer> ccsizes = g.getCcSizes();
        assertEquals(3, ccsizes.size());
        assertTrue(ccsizes.contains(1));
        assertTrue(ccsizes.contains(2));
        assertTrue(ccsizes.contains(3));

        g = gr.fromFile("testResources/medium.metis");
        ccsizes = g.getCcSizes();
        assertEquals(1, ccsizes.size());
        assertTrue(ccsizes.contains(g.getNodeCount()));

        g = gr.fromFile("testResources/medium2.metis");
        ccsizes = g.getCcSizes();
        assertEquals(1, ccsizes.size());
        assertTrue(ccsizes.contains(g.getNodeCount()));

        g = gr.fromFile("testResources/disconnected.metis");
        ccsizes = g.getCcSizes();
        assertEquals(2, ccsizes.size());
        assertTrue(ccsizes.contains(1));
        assertTrue(ccsizes.contains(3));
    }

    @Test
    void getOverlappingNodes() throws IOException {
        GraphReader gr = new GraphReader();
        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.Matrix());
        Graph med = gr.fromFile(medium);

        gr.setInputFormat(new GraphReader.NodeList(med));
        gr.setReturnFormat(new GraphReader.Subgraph());
        InducedSubgraph sub = (InducedSubgraph) gr.fromFile(mediumNl);
        InducedSubgraph sub2 = (InducedSubgraph) gr.fromFile(mediumNl2);

        ArrayList<Integer> overlapping = sub.getOverlappingNodes(med);
        ArrayList<Integer> overlapping2 = sub.getOverlappingNodes(med);

        assertEquals(overlapping.size(), overlapping2.size());
        assertEquals(sub.n, overlapping2.size());
        assertTrue(overlapping.contains(0));
        assertTrue(overlapping.contains(1));
        assertTrue(overlapping.contains(2));
        assertTrue(overlapping.contains(3));
        assertTrue(overlapping.contains(13));
        assertTrue(overlapping.contains(10));

        ArrayList<Integer> subOverlapping = sub.getOverlappingNodes(sub2);
        assertEquals(4, subOverlapping.size());
        assertTrue(subOverlapping.contains(1));
        assertTrue(subOverlapping.contains(2));
        assertTrue(subOverlapping.contains(3));
        assertTrue(subOverlapping.contains(10));

    }

    @Test
    void getNodeOverlapPercent() throws IOException {
        GraphReader gr = new GraphReader();
        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.Matrix());
        Graph med = gr.fromFile(medium);

        gr.setInputFormat(new GraphReader.NodeList(med));
        gr.setReturnFormat(new GraphReader.Subgraph());
        InducedSubgraph sub = (InducedSubgraph) gr.fromFile(mediumNl);
        InducedSubgraph sub2 = (InducedSubgraph) gr.fromFile(mediumNl2);

        assertEquals(1d, sub.getNodeOverlapPercent(med));
        assertEquals(2d/3, sub.getNodeOverlapPercent(sub2));
        assertEquals(0.8d, sub2.getNodeOverlapPercent(sub));
    }

    @Test
    void getNonoverlappingNodes() throws IOException {
        GraphReader gr = new GraphReader();
        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.Matrix());
        Graph med = gr.fromFile(medium);

        gr.setInputFormat(new GraphReader.NodeList(med));
        gr.setReturnFormat(new GraphReader.Subgraph());
        InducedSubgraph sub = (InducedSubgraph) gr.fromFile(mediumNl);
        InducedSubgraph sub2 = (InducedSubgraph) gr.fromFile(mediumNl2);

        assertEquals(2, sub.getNonoverlappingNodes(sub2).size());
        assertTrue(sub.getNonoverlappingNodes(sub2).contains(0));
        assertTrue(sub.getNonoverlappingNodes(sub2).contains(13));
        assertEquals(1, sub2.getNonoverlappingNodes(sub).size());
        assertEquals(16, sub2.getNonoverlappingNodes(sub).get(0));
        assertEquals(0, sub2.getNonoverlappingNodes(sub2).size());
    }
}