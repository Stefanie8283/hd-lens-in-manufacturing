package Utils;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DepthFirstSearch {

    /*public static void main(String[] args) throws DiffException {
        HFMs fsm = new HFMs().readJsonFiles("target/data/02_HLFM05.json");
        List<LFMs.Asset> allAssets = fsm.getSceneAssets();
        List<Asset> rest = filterList(allAssets, (LFMs.Asset s) -> !s.getConnectedTo().isEmpty());

        //Generate a graph based on connectedTo relationship
        Graph<String, DefaultEdge> graph = findEdge(rest,fsm);

        System.out.println("All Vertex: "+graph.vertexSet());
        System.out.println("-----------------All Edges------------------ ");
        graph.edgeSet().forEach(s-> System.out.println(graph.getEdgeSource(s)+" -> "+graph.getEdgeTarget(s)));

        System.out.println("-----------------Path started from B1:-----------------");
        dsfTraverseGraph(graph,"B1");

        System.out.println("Find the shortest path from B1 to B2");
        DijkstraShortestPath<String, DefaultEdge> dijkstraAlg =
                new DijkstraShortestPath<String, DefaultEdge>(graph);
        ShortestPathAlgorithm.SingleSourcePaths<String, DefaultEdge> iPaths = dijkstraAlg.getPaths("B1");
        System.out.println(iPaths.getPath("B2") + "\n");

    }

    //In the set of rest, find all the connected Assets sets
    public static Graph<String, DefaultEdge> findEdge(List<Asset> rest, HFMs fsm){
        Graph<String,DefaultEdge> graph = new SimpleDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
        rest.forEach(a -> graph.addVertex(a.getId()));
        Iterator<Asset> restIterator = rest.iterator();
        while(restIterator.hasNext()){
            Asset current = restIterator.next();
            Asset match = fsm.getConectedAsset(current);
            if(!graph.vertexSet().contains(match.getId()))
                graph.addVertex(match.getId());
            graph.addEdge(current.getId(),match.getId());
        }
        return graph;
    }*/

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
