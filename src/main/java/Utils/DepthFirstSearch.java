package Utils;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DepthFirstSearch {

    public static void dsfTraverseGraph(Graph<String, DefaultEdge> graph, String start)
    {
        Iterator<String> iterator = new DepthFirstIterator<>(graph, start);
        while (iterator.hasNext()) {
            String uri = iterator.next();
            System.out.println(uri);
        }
    }

    /** Depth-first Finding (DSF) algorithm in Graph, finding the path from a starting node */
    public static List<String> dsfGraph(Graph<String, DefaultEdge> graph, String start)
    {
        List<String> path = new ArrayList<>();
        Iterator<String> iterator = new DepthFirstIterator<>(graph, start);
        while (iterator.hasNext()) {
            String uri = iterator.next();
            path.add(uri); }
        return path;
    }

}
