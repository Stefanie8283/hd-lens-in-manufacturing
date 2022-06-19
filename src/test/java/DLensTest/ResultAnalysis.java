package DLensTest;

/* This class analyse the result from Delta-lens
  Designed by Qunfen Qi
  Date: 2021-01-12
  */

import AFMs.HFMs;
//import Utils.Pair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import Utils.Triple;
import org.apache.commons.lang3.Range;

import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static DLensTest.LFMDLens.testBufferCap;

public class ResultAnalysis {

    /** A total function to implement two types of comparison between the estimated map and the actual map */
    public static Object[] compareResult(String testNo,Object[] dPutResult) throws IOException{
        TreeMap<String, Map<String,Pair<Double, Double>>> estAggMap = (TreeMap<String, Map<String,Pair<Double, Double>>>) dPutResult[0];
        TreeMap<String, Map<String,Pair<Double, Double>>> realAggMap = (TreeMap<String, Map<String,Pair<Double, Double>>>) dPutResult[1];
        List<String> aggBuffers= (List<String>) dPutResult[2];
        HFMs hfm = (HFMs) dPutResult[4];

        //Type 1 comparison:
        TreeMap<String, List<Pair<String, Double>>> typeI = compareTypeI(estAggMap,realAggMap,aggBuffers);

        TreeMap<Pair<Double, Double>,Map<String,HashSet<String>>> estConverted = convert(new TreeMap<>(estAggMap));
        TreeMap<Pair<Double, Double>,Map<String,HashSet<String>>> realConverted = convert(new TreeMap<>(realAggMap));

        HashSet<Double> timeStamps = findTimeStamps(estConverted,realConverted);

        TreeMap<Double,Map<String,HashSet<String>>> estParts = partsOnBuffer(estConverted,timeStamps);
        TreeMap<Double,Map<String,HashSet<String>>> realParts = partsOnBuffer(realConverted,timeStamps);

        int errors = testBufferCap(estParts,hfm,testNo); // test the bufferCap constraint

        //Type 2 comparison:
        TreeMap<Double, List<Pair<String, Integer>>> typeII = compareTypeII(estParts,realParts,aggBuffers);

        Object[] result = new Object[2];
        result[0] = typeI;
        result[1] = typeII;

        return result;
    }

    /** Type I comparison: for each part, compare each time stamp which the part passing through a buffer */
    public static TreeMap<String, List<Pair<String, Double>>> compareTypeI(TreeMap<String, Map<String,Pair<Double, Double>>> estMap, TreeMap<String, Map<String,Pair<Double, Double>>> realMap,List<String> aggBuffers){

        TreeMap<String, List<Pair<String, Double>>> TimeBias = new TreeMap<>();

        for(String eachPart:estMap.keySet()){
            List<Pair<String, Double>> listBias = new ArrayList<>();
            for(String buffer : aggBuffers){
                if(estMap.get(eachPart).containsKey(buffer) )
                {
                    Map<String,Pair<Double,Double>> bufferTime = estMap.get(eachPart);
                    Map<String,Pair<Double,Double>> realTime = realMap.get(eachPart);
                    Pair<Double, Double> estRange = bufferTime.get(buffer);
                    Pair<Double, Double> realRange = realTime.get(buffer);
                    Double exitTime = estRange.getRight();
                    Double exitTimeLog = realRange.getRight();
                    Double bias = (double) Math.round((exitTime - exitTimeLog) * 1000) / 1000 ;
                    Pair<String, Double> biaData = new MutablePair<>(buffer,bias);
                    listBias.add(biaData);
                }
            }
            if(!listBias.isEmpty())
                TimeBias.put(eachPart,listBias);
        }
        System.out.println("-Difference between the estimated time stamp and actual time stamp for all of the parts-");
        System.out.printf("%-20s %-20s%n", "Parts", "Bias");
        TimeBias.keySet().forEach(p -> System.out.printf("%-20s %-20s%n", p, TimeBias.get(p)));
        return TimeBias;
    }


    /** Type II comparison: For each time stamp, compare the estimated parts on each buffer */
    public static TreeMap<Double, List<Pair<String, Integer>>> compareTypeII (TreeMap<Double,Map<String,HashSet<String>>> estParts, TreeMap<Double,Map<String,HashSet<String>>> realParts,List<String> aggBuffers)
    {
        TreeMap<Double, List<Pair<String, Integer>>> eachPartBias = new TreeMap<>();

        System.out.printf("%-20s %-20s%n", "Time", "Actual vs Estimated");

        for (Double time : realParts.keySet())
        {
            Map<String, HashSet<String>> real = realParts.get(time);
            //Map<String, HashSet<String>> RealBufferMap = realParts.get(time);
            System.out.printf("%-20s %-20s%n", time, "Actual: "+real);

            List<Pair<String, Integer>> listBias = new ArrayList<>();

            if(estParts.containsKey(time)){
                Map<String, HashSet<String>> est = estParts.get(time);
                for (String eachBuffer : aggBuffers) {
                    int estI = 0;
                    int realI = 0;
                    //For each buffer, check the difference between parts numbers
                    if(est.containsKey(eachBuffer)) {
                        estI = est.get(eachBuffer).size();}
                    if(real.containsKey(eachBuffer)) {
                        realI = real.get(eachBuffer).size();}
                    Integer bias = estI - realI;
                    listBias.add(new MutablePair<>(eachBuffer,bias));
                }
                eachPartBias.put(time,listBias);
               System.out.printf("%-20s %-20s%n", "", "Estimate: "+est);
            }
        }
        System.out.println("--Difference between the estimated and actual parts on buffers--");
        System.out.printf("%-20s %-20s%n", "Time", "Bias");
        eachPartBias.keySet().forEach(p -> System.out.printf("%-20s %-20s%n", p, eachPartBias.get(p)));
        return eachPartBias;
    }

    /** Get a TreeMap for each Time Stamp, on each buffer, the list of parts which sit on this buffer*/
    public static TreeMap<Double,Map<String,HashSet<String>>> partsOnBuffer (TreeMap<Pair<Double, Double>,Map<String,HashSet<String>>> map, HashSet<Double> timeStamps){
        TreeMap<Double, Map<String, HashSet<String>>> output = new TreeMap<>();

        for (Double time : timeStamps) {
            //Get output with the same set of time stamps of the log file
            for(Pair<Double,Double> pair: map.keySet()){
                Map<String,HashSet<String>> list = map.get(pair);
                Range<Double> myRange = Range.between(pair.getLeft(), pair.getRight());
                if (myRange.contains(time))
                    output.put(time, list);
            }
        }
        return output;
    }

    /** Finding all possible overlaps between a pair of time stamp */
    public static TreeMap<Pair<Double, Double>, Map<String,HashSet<String>>> findOverLap(TreeMap<Pair<Double, Double>,Map<String,HashSet<String>>> tree) {
        TreeMap<Pair<Double, Double>, Map<String,HashSet<String>>> output = new TreeMap<>();
        List<Triple<Double, Map<String,HashSet<String>>, Boolean>> triList = new ArrayList<>();
        // Make a list of triples from the map:
        for(Pair<Double,Double> eachItem: tree.keySet()){
            Map<String,HashSet<String>> value = tree.get(eachItem);
            triList.add(Triple.of(eachItem.getLeft(), value,false));  // false - the start of a range
            triList.add(Triple.of(eachItem.getRight(), value,true)); // true - the end of a range
        }
        //Sorting the triList using first the order of double, then false, ture
        triList.sort((Comparator<Triple>) (x, y) -> {
            if (Double.compare((double) x.getLeft(), (double) y.getLeft()) == 0) {
                if ((boolean) x.getRight() && !((boolean) y.getRight())) return 1;
                else if (!((boolean) x.getRight()) && (boolean) y.getRight()) return -1;
                else return 0;
            } else
                return Double.compare((double) x.getLeft(), (double) y.getLeft());
        });

        Triple<Double, Map<String,HashSet<String>>, Boolean> pre = null;
        List<Map<String,HashSet<String>>> S = new ArrayList<>(); // an empty middle attributes holder

        //current triple (m,-,f)
        for (Triple<Double, Map<String, HashSet<String>>, Boolean> curTri : triList) {
            Double m = curTri.getLeft();
            Boolean f = curTri.getRight();
            if (pre != null) {
                Map<String, HashSet<String>> preValue = pre.getMiddle();
                //pre triple (n,preValue,e)
                Double n = pre.getLeft();
                Boolean e = pre.getRight();
                Double nP = 0.0;  Double mP = 0.0; //Two possible overlap numbers

                if (e.equals(false)) {
                    S.add(preValue);
                    nP = n; }
                else if (e.equals(true)) {
                    S.remove(preValue);
                    nP = n; }
                if (f.equals(false)) {
                    mP = m;}
                else if (f.equals(true)) {
                    mP = m; }

                if (Double.compare(nP, mP) <= 0) {
                    Pair<Double, Double> newpair = new MutablePair<>(nP, mP);
                    Map<String, HashSet<String>> newE = merge(new ArrayList<>(S));
                    output.put(newpair, newE);
                }
            }
            pre = curTri;
        }
        return findDu(new TreeMap<>(output));
    }

    /** Eliminate the affect of a part "exit" a buffer, but it still counts as the part within the buffer,
     * by remove any two consecutive ranges, with the end of the first = the start of the second */
    public static TreeMap<Pair<Double, Double>, Map<String,HashSet<String>>> findDu(TreeMap<Pair<Double, Double>, Map<String,HashSet<String>>> input){
        List<Pair<Double,Double>> keyList = new ArrayList<>(input.keySet());
        for (Pair<Double, Double> key: keyList) {
            Double first = key.getLeft();
            Double second = key.getRight();
                if (Double.compare(first, second) == 0)
                    input.remove(key);
        }
        return input;
    }

    /** Merge a list of "parts on buffer" Maps to a single map */
    public static Map<String, HashSet<String>> merge(List<Map<String, HashSet<String>>> maps) {
        BiFunction<HashSet<String>, HashSet<String>, HashSet<String>> mergeSet
                = (a, b) -> { a.addAll(b); //a.hashCode();
                return a; };

        return maps.stream().flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> new HashSet<>(entry.getValue()),
                        mergeSet::apply));
    }

    /** Convert the format of a TreeMap with key of Time Range */
    public static TreeMap<Pair<Double, Double>,Map<String,HashSet<String>>> convert(TreeMap<String, Map<String,Pair<Double, Double>>> map){
        TreeMap<Pair<Double, Double>,Map<String,HashSet<String>>> output = new TreeMap<>();
        for(String eachPart: map.keySet()){
            Map<String,Pair<Double, Double>> bufferTimeMap = map.get(eachPart);
            for(String eachBuffer: bufferTimeMap.keySet()) {
                Pair<Double, Double> li =bufferTimeMap.get(eachBuffer);
                output.computeIfAbsent(li,v->new TreeMap<>()).computeIfAbsent(eachBuffer, s-> new HashSet<>()).add(eachPart);
            }
        }

        return findOverLap(output);
    }

    /** Combining all time stamps from estimated map and actual map for later comparison */
    public static HashSet<Double> findTimeStamps (TreeMap<Pair<Double, Double>,Map<String,HashSet<String>>> map1, TreeMap<Pair<Double, Double>,Map<String,HashSet<String>>> map2){
        HashSet<Double> time = new HashSet<>();
        map1.keySet().forEach(range -> {
            time.add(range.getLeft());
            time.add(range.getRight()); });
        map2.keySet().forEach(range -> {
            time.add(range.getLeft());
            time.add(range.getRight()); });
        return time;
    }

}
