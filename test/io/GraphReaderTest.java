package io;

import graph.Graph;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

class GraphReaderTest {
    @Test
    public void testNodeList() throws Exception {
        GraphReader gr = new GraphReader();
        gr.setReturnFormat(new GraphReader.List());
        gr.setInputFormat(new GraphReader.Metis());
        Graph g = gr.fromFile("resources/");
        gr.setInputFormat(new GraphReader.NodeList(g));
        //assertThrows()
    }

}