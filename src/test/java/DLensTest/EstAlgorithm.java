package DLensTest;

import AFMs.Asset;
import AFMs.HFMs;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.Range;

import java.util.*;

import static AFMs.HFMs.getCapAndTime;
import static DLensTest.ResultAnalysis.convert;
import static DLensTest.ResultAnalysis.findTimeStamps;

public class EstAlgorithm {

    /** An estimation algorithm which comply with the buffer cap restriction */
    public static TreeMap<String,Map<String,Pair<Double,Double>>> estAlgorithm (TreeMap<String,Map<String,Pair<Double,Double>>> logMap, TreeMap<String, List<String>> aggList, HFMs hfm){
        TreeMap<String,Map<String,Pair<Double,Double>>> estimated = new TreeMap<>();

        List<Pair<String, Double>> partList = sortPartList(logMap);

        for(int p=0;p<partList.size();p++)
        {
            String eachPart = partList.get(p).getLeft();
            Map<String, Pair<Double, Double>> newEst = new TreeMap<>();

            Map<String, Pair<Double, Double>> bufferTime = logMap.get(eachPart);
            for (String eachBuffer : bufferTime.keySet()) {
                List<String> subBufList = aggList.get(eachBuffer);

                int bufferCap = getCapAndTime(subBufList, hfm).first;
                double minTaskTime = getCapAndTime(subBufList, hfm).second;
                double actualTime;

                Pair<Double, Double> range = bufferTime.get(eachBuffer);
                Double start = range.getLeft();
                Double end = range.getRight();
                actualTime = end - start;

                double k = actualTime - minTaskTime;
                int size = subBufList.size();
                double estStart = 0.0;
                double estEnd = 0.0;

                for (int i = 0; i < size; i++) {
                    String buffer = subBufList.get(i);
                    Asset asset = hfm.getAssetsByID(buffer);
                    Double thisTaskTime = hfm.getAssignmentToTaskTime(asset);

                    if (p == 0) {
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
                            estEnd = (double) Math.round(temp * 100) / 100;
                        }
                    }
                    if (p > 0)// For the rest of the Buffers
                    {
                        if (i == 0)  // the first
                            estStart = start;
                        if (i == (size - 1))  // the last sub-buffer
                            estEnd = end;
                        if (i < size - 1) {
                            String nextBuffer = subBufList.get(i + 1);
                            Asset nextAsset = hfm.getAssetsByID(nextBuffer);
                            Integer nextCap = nextAsset.getBufferCap();
                            // 1. get the number of parts (n) on each sub-buffers at the time point of range.first+ minimum TaskTime
                            TreeMap<Pair<Double, Double>, Map<String, HashSet<String>>> estConverted = convert(new TreeMap<>(estimated));
                            Double time = estStart + thisTaskTime;
                            Integer n = statusOnTime(estConverted, time, nextBuffer); // number of parts on the next buffer
                            //System.out.println(eachPart + " - Default time: " + time + " buffer: " + buffer + " next: " + nextBuffer + ", n = " + n + " Cap: " + nextCap);

                            // 4. if n = Cap, find the time stamp when n < Cap, by search each ordered time stamp one by one util n < Cap is satisfied;
                            if (n == nextCap) {
                                // find the next time stamp by increment if the condition is satisfied
                                //time = findPass(nextCap,time,nextBuffer,estConverted);
                                HashSet<Double> stamps = findTimeStamps(estConverted, new TreeMap<>());
                                List<Double> stampList = new ArrayList<>(stamps);
                                stampList.sort(Comparator.naturalOrder());
                                time = findPass(nextCap, time, nextBuffer, estConverted, stampList);
                                //System.out.println("estEnd: "+estEnd);
                            }

                            // 5. if n > Cap, send the warning;
                            else if (n > nextCap) {
                                //System.err.println("At time " + time + ": " + nextBuffer + " hosting parts of " + n + " > bufferCap " + nextCap);
                                // get back to a time stamp where < time, and re-arrange the time for this buffer
                                //estEnd = reverse(nextCap,time,nextBuffer,estConverted);
                                //System.out.println("reverse Time: "+estEnd);
                            }
                            estEnd = time;
                            //System.out.println("estEnd: " + estEnd);
                        }
                    }
                    newEst.put(buffer, new MutablePair<>(estStart, estEnd));
                    estStart = (double) Math.round((estEnd) * 100) / 100;
                }
                estimated.put(eachPart, newEst);
            }
        }

        System.out.println("New Algorithm Estimated: ");
        estimated.keySet().forEach(t -> System.out.println(t+": "+ estimated.get(t)));
        return estimated;
    }

    /** A recursion algorithm for find the right time stamp for a part moving in to a buffer */
    public static Double findPass (Integer cap, Double time, String buffer,TreeMap<Pair<Double, Double>,Map<String,HashSet<String>>> est,List<Double> stampList){
        for(Double incr: stampList){
            if(Double.compare(incr,time)>0) {
                time = incr;
                int n = statusOnTime(est,time,buffer);
                if(n<cap)
                    return time;
                else  return findPass(cap,time,buffer,est,stampList); }
        }
        return time;
    }


    public static Double reverse (Integer cap, Double time, String buffer,TreeMap<Pair<Double, Double>,Map<String,HashSet<String>>> est){
        Double temp = (double) Math.round((time - 0.50) * 100) / 100;
        Integer n = statusOnTime(est,temp,buffer);
        if(n<cap){
            System.out.println("reverse Time: "+temp+" n="+n);
            return temp;}
        else return reverse(cap,temp,buffer,est);
        //return time;
    }

    /** Sorting the map with 1) the order of buffers; 2) enter time; 3) exit time*/
    public static List<Pair<String,Double>> sortPartList (TreeMap<String, Map<String,Pair<Double, Double>>> map){
        List<Pair<String,Double>> partList = new ArrayList<>();

       for(String eachPart: map.keySet()){
            Map<String,Pair<Double, Double>> value = map.get(eachPart);
            List<Double> times = new ArrayList<>();
            for(String buffer: value.keySet()){
                Pair<Double,Double> timeRange = value.get(buffer);
                times.add(timeRange.getLeft());
                times.add(timeRange.getRight());
            }
            double min = times.stream().min(Comparator.comparing(Double::doubleValue)).get();
            partList.add(new MutablePair<>(eachPart,min));
        }

        partList.sort(Comparator.comparing(x -> x.getRight()));

        return partList;
    }

    /** Find the number of parts on a buffer at a time stamp */
    public static Integer statusOnTime(TreeMap<Pair<Double, Double>,Map<String,HashSet<String>>> map, Double time, String buffer){
        Integer partsNo = 0;
        for(Pair<Double, Double> pair: map.keySet()){
            Range<Double> myRange = Range.between(pair.getLeft(), pair.getRight());
            if(myRange.contains(time)){
                Map<String,HashSet<String>> thisBuffer = map.get(pair);
                for(String any: thisBuffer.keySet()){
                    if(any.equals(buffer))
                        partsNo = thisBuffer.get(any).size();
                }
            }
        }
        return partsNo;
    }
}
