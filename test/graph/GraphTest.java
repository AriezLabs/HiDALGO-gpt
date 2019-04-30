package graph;

import io.GraphReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class GraphTest {
    Graph[] gs;
    ArrayList<Integer> neighborsOfZero;

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
        Graph g = gr.fromFile("resources/medium.metis");
        assertEquals(0.5781, g.getEigenvalue(), 0.0001);
        g = gr.fromFile("resources/medium2.metis");
        assertEquals(0.4817, g.getEigenvalue(), 0.0001);
    }
}