package tasks;

import graph.Graph;
import io.GraphReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FilterStarlikeTest {

   @Test
   void test () throws IOException {
       GraphReader gr = new GraphReader();
       gr.setInputFormat(new GraphReader.Metis());
       gr.setReturnFormat(new GraphReader.List());
       Graph t1 = gr.fromFile("testResources/filtertest1.txt");
       Graph t2 = gr.fromFile("testResources/filtertest2.txt");
       Graph removed = t2.removeStronglyConnectedNodes(0.74);
       System.out.println(removed);
   }

}