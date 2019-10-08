package io;

import graph.Graph;
import graph.InducedSubgraph;
import index.MergeCandidate;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

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

    @Test
    void testGraphViz() throws IOException {
        GraphWriter gw = new GraphWriter();
        gw.setFormat(new GraphWriter.Metis());

        GraphReader gr = new GraphReader();
        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.List());
        Graph g = gr.fromFile("testResources/medium.metis");

        gr.setInputFormat(new GraphReader.NodeList(g));
        gr.setReturnFormat(new GraphReader.Subgraph());
        InducedSubgraph a = (InducedSubgraph) gr.fromFile("testResources/medium.nl");
        InducedSubgraph b = (InducedSubgraph) gr.fromFile("testResources/medium.nl.2");

        ArrayList<InducedSubgraph> subs = new ArrayList<>();
        subs.add(a);
        subs.add(b);

        MergeCandidate mc = new MergeCandidate(a,subs);
        mc.next();
        System.out.println(gw.toGraphViz(mc));

        // pt 2

        gr.setInputFormat(new GraphReader.Metis());
        gr.setReturnFormat(new GraphReader.List());
        g = gr.fromFile("testResources/small.metis");

        gr.setInputFormat(new GraphReader.NodeList(g));
        gr.setReturnFormat(new GraphReader.Subgraph());
        a = (InducedSubgraph) gr.fromFile("testResources/small1.nl");
        b = (InducedSubgraph) gr.fromFile("testResources/small2.nl");

        subs = new ArrayList<>();
        subs.add(a);
        subs.add(b);

        mc = new MergeCandidate(a,subs);
        mc.next();
        gw = new GraphWriter();
        System.out.println(g.hasEdge(5, 3));
        System.out.println(gw.toGraphViz(mc));
    }
}