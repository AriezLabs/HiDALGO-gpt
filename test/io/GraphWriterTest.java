package io;

import graph.Graph;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GraphWriterTest {

    @Test
    void testString() throws IOException {
        GraphReader gr = new GraphReader();
        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.Matrix());
        Graph g = gr.fromFile("testResources/medium.metis");
        gr.setInputFormat(new GraphReader.NodeList(g));
        System.out.println(gr.fromFile("testResources/medium.nl"));
    }
}