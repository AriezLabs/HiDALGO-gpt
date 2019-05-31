package io;

import graph.Graph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * tests: metis graphs (ok, too few/many nodes, bad edge count)
 * node list
 */
class GraphReaderTest {

    @ParameterizedTest
    @ValueSource(strings = {"tiny.metis", "medium.metis"})
    public void testTiny(String s) throws Exception {
        String testgraph = "testResources/" + s;

        GraphReader gr = new GraphReader();
        gr.setReturnFormat(new GraphReader.List());
        gr.setInputFormat(new GraphReader.Metis());

        Graph g = gr.fromFile(testgraph);

        GraphWriter gw = new GraphWriter();
        gw.setFormat(new GraphWriter.Metis());
        String result = gw.toString(g);

        StringBuilder original = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(testgraph)))) {
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("%"))
                    original.append(line.trim()).append("\n");
            }
        }

        assertEquals(result, original.toString());
        assertEquals(s.split("\\.")[0], g.getName());
    }

    @Test
    public void testBadEdges() {
        String testgraph = "resources/badEdges.metis";

        GraphReader gr = new GraphReader();
        gr.setReturnFormat(new GraphReader.List());
        gr.setInputFormat(new GraphReader.Metis());

        assertThrows(FileFormatException.class, () -> gr.fromFile(testgraph));
    }

    @Test
    public void testBadNodes() {
        String testgraph = "resources/tooManyNodes.metis";

        GraphReader gr = new GraphReader();
        gr.setReturnFormat(new GraphReader.List());
        gr.setInputFormat(new GraphReader.Metis());

        assertThrows(IllegalArgumentException.class, () -> gr.fromFile(testgraph));

        String testgraph2 = "resources/tooFewNodes.metis";

        assertThrows(FileFormatException.class, () -> gr.fromFile(testgraph2));
    }

    @Test
    void testNodeList() throws IOException {
        String testgraph = "resources/tiny.metis";
        String nodelist = "resources/tiny.nl";

        GraphReader gr = new GraphReader();
        gr.setReturnFormat(new GraphReader.List());
        gr.setInputFormat(new GraphReader.Metis());
        Graph g = gr.fromFile(testgraph);

        gr.setReturnFormat(new GraphReader.Subgraph());
        gr.setInputFormat(new GraphReader.NodeList(g));

        Graph sub = gr.fromFile(nodelist);

        gr.setReturnFormat(new GraphReader.List());
        Graph subMat = gr.fromFile(nodelist);

        for (int i = 0; i < sub.getNodeCount(); i++) {
            for (int j = 0; j < sub.getNodeCount(); j++) {
                assertEquals(sub.hasEdge(i, j), subMat.hasEdge(i, j));
            }
        }

        assertTrue(subMat.hasEdge(0, 1));
        assertTrue(subMat.hasEdge(1, 2));
        assertEquals(4, subMat.getEdgeCount());
        assertEquals(3, subMat.getNodeCount());
    }

    @Test
    void testNodeListWithEv() throws IOException {
        String testgraph = "testResources/tiny.metis";
        String nodelist = "testResources/tiny.nlev";

        GraphReader gr = new GraphReader();
        gr.setReturnFormat(new GraphReader.List());
        gr.setInputFormat(new GraphReader.Metis());
        Graph g = gr.fromFile(testgraph);

        gr.setReturnFormat(new GraphReader.Subgraph());
        gr.setInputFormat(new GraphReader.NodeListWithEvs(g));

        Graph sub = gr.fromFile(nodelist);

        gr.setReturnFormat(new GraphReader.List());
        Graph subMat = gr.fromFile(nodelist);

        for (int i = 0; i < sub.getNodeCount(); i++) {
            for (int j = 0; j < sub.getNodeCount(); j++) {
                assertEquals(sub.hasEdge(i, j), subMat.hasEdge(i, j));
            }
        }

        assertTrue(subMat.hasEdge(0, 1));
        assertTrue(subMat.hasEdge(1, 2));
        assertEquals(4, subMat.getEdgeCount());
        assertEquals(3, subMat.getNodeCount());
        assertEquals(1337, sub.getEigenvalue());
        assertEquals(1337, subMat.getEigenvalue());
    }
}