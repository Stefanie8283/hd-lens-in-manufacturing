package DLensTest;

/* This class defines a Delta-lens structure between HFM and LFM
  Designed by Qunfen Qi
  Date: 2021-01-12
  */

import AFMs.Asset;
import AFMs.HFMs;
import AssemblySt.ReadLogs;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.io.IOException;
import java.util.*;

import static AFMs.HFMs.getCapAndTime;
import static AssemblySt.ReadLogs.converToPair;
import static AssemblySt.ReadLogs.getBuffers;
import static DLensTest.QueueAlgorithm.estAlgorithm;
import static Utils.DepthFirstSearch.dsfGraph;
import static Utils.Predicate.filterList;


public class LFMDLens {

    public LFMDLens() { }

    /** Order a list of strings (connected buffers) by its connectedTo relationship */
    public static List<String> orderByConnectedTo(List<String> unordered, HFMs fsm){
        Graph<String,DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
        unordered.forEach(graph::addVertex);
        for (String s : unordered) {
            Asset current = fsm.getAssetsByID(s);
            Asset match = fsm.getConectedAsset(current);
            if (graph.vertexSet().contains(match.getId()))
                graph.addEdge(current.getId(), match.getId());
        }
        //Find the source node:
        String node = null;
        for(String v: graph.vertexSet()) {
            if(graph.incomingEdgesOf(v).isEmpty())
                node=v;
        }
        return dsfGraph(graph, node);
    }

    /** A new Get Function with a recursion of finding any possible composition */
    public static TreeMap<String, List<String>> getComMap(TreeMap<String, List<String>> getMap,HFMs level1){
        TreeMap<String, List<String>> getgetMap = new TreeMap<>();
        List<Asset> filter = filterList(level1.getSceneAssets(), (Asset s) -> s.getParentObject()!=null);
        getMap.keySet().forEach(key -> getMap.get(key).forEach(sub -> {
            Asset asset = level1.getAssetsByID(sub);
            for(Asset as: filter) {
                if(as.getParentObject().equals(asset.getId()))
                    getgetMap.computeIfAbsent(key, a-> new ArrayList<>()).add(as.getId()); }
        }));

        System.out.println("--Full Buffer Map After Composition--");
        System.out.println(getgetMap);
        return getgetMap;
    }

    /** The Get Function: get the relationship (map) between HFM and LFM */
    public static TreeMap<String, List<String>> getMap(HashSet<String> buffers, HFMs fsm){
        TreeMap<String, List<String>> output = new TreeMap<>();
        List<Asset> filter = filterList(fsm.getSceneAssets(), (Asset s) -> s.getParentObject()!=null);
        for(String buffer: buffers){
            Asset as = fsm.getAssetsByID(buffer);
            for(Asset asset: filter){
                if(asset.getParentObject().equals(as.getId()))
                    output.computeIfAbsent(buffer, a-> new ArrayList<>()).add(asset.getId());
            }
        }
        System.out.println("The Get Map:"+ output);
        return output;
    }

    /** Order all of the sub-buffers in the aggregation map, with "conncetedTo" sequence */
    public static List<String> orderBuffers(TreeMap<String, List<String>> aggList, HFMs fsm){
        List<String> fullAggList = new ArrayList<>();
        for(String snode:aggList.keySet()){
            List<String> ordered = orderByConnectedTo(aggList.get(snode),fsm);
            fullAggList.addAll(ordered);
        }
        return fullAggList;
    }


    /** Get the aggregation list from the full getMap */
    public static TreeMap<String, List<String>> getAggList(TreeMap<String, List<String>> getMap,HFMs hfm){
        TreeMap<String, List<String>> aggList = new TreeMap<>();
        for(String buffer: getMap.keySet()){
            List<String> subBuffers = getMap.get(buffer);
            subBuffers = orderByConnectedTo(subBuffers,hfm);
            if(subBuffers.size()>1)
                aggList.put(buffer,subBuffers);
        }
        System.out.println("The Aggregation List: "+ aggList);
        return aggList;
    }

    /** The algorithm to estimate the backward-mapping: allocate each aggregated buffer a time slot */
    public static TreeMap<String,Map<String,Pair<Double,Double>>> estAggBufferTime(TreeMap<String,Map<String,Pair<Double,Double>>> logMap, TreeMap<String,List<String>> aggList,HFMs hfm){
        TreeMap<String,Map<String,Pair<Double,Double>>> estimated = new TreeMap<>();

        for(String eachPart: logMap.keySet()) {  //compB.1
            Map<String,Pair<Double,Double>> newEst = new TreeMap<>();

            Map<String, Pair<Double, Double>> bufferTime =  logMap.get(eachPart);
            for (String eachBuffer : bufferTime.keySet()) {
                List<String> thisList = aggList.get(eachBuffer);

                int bufferCap = getCapAndTime(thisList,hfm).first;
                double minTaskTime = getCapAndTime(thisList,hfm).second;
                double actualTime;

                Pair<Double, Double> range = bufferTime.get(eachBuffer);
                Double start = range.getLeft();
                Double end = range.getRight();
                actualTime = end - start;
                if (actualTime > 0) {
                    if ((double) Math.round(actualTime * 100) / 100 < minTaskTime) {
                        System.err.println("TaskTime for " + eachPart + " on " + eachBuffer + " is " + actualTime + " < the minimum taskTime " + minTaskTime);
                    }
                }
                double k = actualTime - minTaskTime;
                int size = thisList.size();
                double estStart = 0.0;
                double estEnd;

                for (int i = 0; i < size; i++)
                {
                    String buffer = thisList.get(i);
                    Asset asset = hfm.getAssetsByID(buffer);
                    Double thisTaskTime = hfm.getAssignmentToTaskTime(asset);

                    //Method 3: Precentage of Cap
                    Integer thisTaskCap = asset.getBufferCap();
                    double percent = (double) thisTaskCap / bufferCap;
                    Double addEach;
                    addEach = (double) Math.round(percent * k * 100) / 100;
                    if (i == 0)  // the first
                        estStart = start;
                    if (i == (size - 1))  // the last
                        estEnd = end;
                    else {
                        double temp = estStart + thisTaskTime + addEach;
                        estEnd = (double) Math.round(temp * 100) / 100 ; }

                    newEst.put(buffer, new MutablePair<>(estStart, estEnd));
                    estStart = (double) Math.round((estEnd) * 100) / 100 ;
                }
            }
            estimated.put(eachPart,newEst);
        }
        return estimated;
    }

    /** Evaluation via dput: check for each buffer, at the same time stamp, how many parts (integer n) is on the buffer
     * whether n <= buffercap */
    public static int testBufferCap(TreeMap<Double,Map<String,HashSet<String>>> estMap,HFMs hfm, String number) {
        int i=0;
        for (Double time : estMap.keySet()) {
            Integer bufferCap;
            Map<String, HashSet<String>> bufferMap = estMap.get(time);
            for (String eachBuffer : bufferMap.keySet()) {
                Asset buffer = hfm.getAssetsByID(eachBuffer);
                bufferCap = buffer.getBufferCap();
                //how many parts on the eachBuffer
                Integer real = bufferMap.get(eachBuffer).size();
                if (bufferCap < real){
                    i++;
                    System.err.println("At time " + time + ": " + eachBuffer + " hosting " + real + " parts " + bufferMap.get(eachBuffer) + " > bufferCap " + bufferCap);
                    }
            }
        }
        if(i>0) {
            System.err.println("Error detected in TestCase "+number);
            System.err.println("Total detected errors on Cap Constraint: "+i+"\n");
        }
        return i;
    }

    /** From the actual log on HFM, filter with only aggregated sub-buffers */
    public static TreeMap<String, Map<String,Pair<Double, Double>>> filterAggReal(TreeMap<String, Map<String,Pair<Double, Double>>> map, List<String> aggBuffers){
        TreeMap<String, Map<String,Pair<Double, Double>>> output = new TreeMap<>();

        for(String part: map.keySet()){
            Map<String,Pair<Double, Double>> thisMap = map.get(part);
            for(String buffer: thisMap.keySet()){
                if(aggBuffers.contains(buffer)){
                    Pair<Double,Double> pair = thisMap.get(buffer);
                    output.computeIfAbsent(part,v-> new TreeMap<>()).put(buffer,pair);
                }
            }
        }
        return  output;
    }

    /** From the estimated log on LFM, filter with only aggregated buffers */
    public static TreeMap<String, Map<String,Pair<Double, Double>>> filterAggEst(TreeMap<String, Map<String,Pair<Double, Double>>> map, TreeMap<String,List<String>> aggList){
        TreeMap<String, Map<String,Pair<Double, Double>>> output = new TreeMap<>();
        for(String part: map.keySet()){
            Map<String,Pair<Double, Double>> thisMap = map.get(part);
            for(String buffer: thisMap.keySet()){
                if(aggList.containsKey(buffer)){
                    Pair<Double,Double> pair = thisMap.get(buffer);
                    output.computeIfAbsent(part,v-> new TreeMap<>()).put(buffer,pair);
                }
            }
        }
        return  output;
    }

    /** The Delta-Lens structure between HFM and LFM */
    public static Object[] DLens(String caseNo, String testNo) throws  IOException {
        System.out.println("--------------------------------------For UseCase"+ caseNo+" test number: " + testNo +"--------------------------------------");
        //Load the LFM's log file
        String logFile = "target/data/UseCase"+ caseNo+"/log/"+ caseNo+"_HFM_"+testNo+"_log.txt";
        new ReadLogs();
        TreeMap<String, Map<String,Pair<Double, Double>>> logMap = converToPair(ReadLogs.loadLog(logFile));

        //Load the structure of LFMs
        String jsonFile = "target/data/UseCase"+ caseNo+"/lfm/"+ caseNo+"_HFM_"+testNo+".json";
        new HFMs();
        HFMs hfm = HFMs.readJson(jsonFile);
        HFMs lfm = hfm;

        HashSet<String> allBuffers = getBuffers(logMap); //Get all of the buffers in the log
        //TreeMap<String, List<String>> getmap = getgetMap(getgetMap(getMap(allBuffers,hfm),hfm),hfm);//Get a map of Buffers with sub-buffer
        //TreeMap<String, List<String>> getmap = getgetMap(getMap(allBuffers,hfm),hfm);//Get a map of Buffers with sub-buffer
        TreeMap<String, List<String>> getmap = getMap(allBuffers,hfm);//Get a map of Buffers with sub-buffer

        List<String> allSubBuffers = new ArrayList<>();
        getmap.keySet().forEach(a ->allSubBuffers.addAll(getmap.get(a)));
        TreeMap<String, List<String>> aggList = getAggList(getmap,hfm);//Get the aggregation listf
        List<String> aggBuffers = orderBuffers(aggList,hfm);//Get all of the aggregationed sub-buffers

        //Estimate time stamps for all aggregated buffers
        TreeMap<String, Map<String,Pair<Double, Double>>> estMap = filterAggEst(logMap,aggList);
        //TreeMap<String, Map<String,Pair<Double, Double>>> estAggMap = estAggBufferTime(estMap,aggList,hfm);
        TreeMap<String, Map<String,Pair<Double, Double>>> estAggMap = estAlgorithm(estMap,aggList,aggBuffers,hfm);


        //Load the acutal log data from HFM, to be compared with the estimated log
        String HFMLog = "target/data/UseCase"+ caseNo+"/"+ caseNo+"_HFM_log.txt";
        TreeMap<String, Map<String,Pair<Double, Double>>> realMap;
        new ReadLogs();
        realMap = converToPair(ReadLogs.loadLog(HFMLog));

        //Filter out realMap with only aggregated buffers:
        TreeMap<String, Map<String, Pair<Double, Double>>> realAggMap = new TreeMap<>(filterAggReal(realMap,aggBuffers));

        //Output the result
        Object[] objects = new Object[5];
        objects [0] = estAggMap;
        objects [1] = realAggMap;
        objects [2] = aggBuffers;
        objects [3] = allSubBuffers;
        objects [4] = hfm;
        return objects;
    }
}

