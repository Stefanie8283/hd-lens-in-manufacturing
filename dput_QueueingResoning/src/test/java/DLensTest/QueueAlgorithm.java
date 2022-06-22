package DLensTest;

/* This class estimate time stamps for aggregated buffers of each part, using queue system
  Designed by Qunfen Qi
  Date: 2021-01-29
  */

import AFMs.Asset;
import AFMs.HFMs;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static DLensTest.LFMDLens.estAggBufferTime;
import static DLensTest.LFMDLens.orderByConnectedTo;
import static DLensTest.ResultAnalysis.*;

public class QueueAlgorithm {

    /** An estimation algorithm which comply with the buffer cap restriction */
    public static TreeMap<String, Map<String, Pair<Double,Double>>> estAlgorithm (TreeMap<String,Map<String,Pair<Double,Double>>> logMap, TreeMap<String, List<String>> aggList, List<String> aggBuffers, HFMs hfm){
        TreeMap<String,Map<String,Pair<Double,Double>>> newLogMap = (estAggBufferTime(logMap,aggList,hfm));

        TreeMap<String,Map<String,Pair<Double,Double>>> estimated = recAlgorithm(newLogMap,aggList,aggBuffers,hfm);

        System.out.println("New Algorithm Estimated: ");
        estimated.keySet().forEach(t -> System.out.println(t+": "+ estimated.get(t)));
        return estimated;
    }

    public static TreeMap<Double,Map<String,HashSet<String>>> convertAll (TreeMap<String, Map<String,Pair<Double, Double>>> estAggMap){
        TreeMap<Pair<Double, Double>,Map<String,HashSet<String>>> estConverted = convert(estAggMap);
        HashSet<Double> timeStamps = findTimeStamps(estConverted,new TreeMap<>());
        return partsOnBuffer(estConverted,timeStamps);
    }

    /* Check if the change of a estAggMap will result a loop: an estimation of a part will be changed and then changed back */
    public static Pair<Double,Map<String,HashSet<String>>> LoopCheck(TreeMap<String, Map<String,Pair<Double, Double>>> estAggMap, HFMs hfm,TreeMap<String, List<String>> aggList, List<String> aggBuffers){
        //Double output = 0.0;
        Pair<Double,Map<String,HashSet<String>>> output = new MutablePair<>(0.0,null);
        TreeMap<Double,Map<String,Pair<HashSet<String>,Integer>>> oldCaps = testBufferCap(estAggMap,hfm);
        TreeMap<String, Map<String,Pair<Double, Double>>> map = Remove(estAggMap,oldCaps,aggList,aggBuffers,hfm);
        TreeMap<Double,Map<String,Pair<HashSet<String>,Integer>>> newCaps = testBufferCap(map,hfm);
        if(!newCaps.isEmpty() && !oldCaps.isEmpty()){
            Double oldTime = oldCaps.firstKey();
            Double newTime = newCaps.firstKey();
            Map<String,Pair<HashSet<String>,Integer>> oldMap = oldCaps.get(oldTime);
            Map<String,Pair<HashSet<String>,Integer>> newMap = newCaps.get(newTime);
            String fOld = new TreeMap<>(oldMap).firstKey();
            String fNew = new TreeMap<>(newMap).firstKey();
            Map<String,HashSet<String>> col = new TreeMap<>();
            col.put(fOld,oldMap.get(fOld).getLeft());
            col.put(fNew,newMap.get(fNew).getLeft());
            if(oldTime.equals(newTime)){
                output = new MutablePair<>(newTime,col);
                System.err.println("Loop detected at "+newTime+" - "+ col);
            }
        }
        return output;
    }

    // This a special algorithm is designed to deal with looped time stamp //
    public static TreeMap<String, Map<String,Pair<Double, Double>>> specialEst(TreeMap<String, Map<String,Pair<Double, Double>>> estAggMap,Pair<Double,Map<String,HashSet<String>>> loop,Map<Double,Map<String,Pair<HashSet<String>,Integer>>> outsideCaps,TreeMap<String, List<String>> aggList, List<String> aggBuffers,HFMs hfm){
        Double time = loop.getLeft();
        Map<String,HashSet<String>> loopMap = loop.getRight();
        List<String> buffersL = new ArrayList<>(loopMap.keySet());
        buffersL=orderByConnectedTo(buffersL, hfm);
        HashSet<String> partsL = new HashSet<>();
        for(String b: buffersL){
            partsL.addAll(loopMap.get(b));
        }

        String eachBuffer = buffersL.get(0);
                //HashSet<String> parts = loopMap.get(eachBuffer);
                List<Pair<String, Double>> partList = orderPartList(estAggMap);
                List<String> sortedParts = sortParts(partsL, partList);
                List<String> allParts = AllParts(partList);
                Asset asset = hfm.getAssetsByID(eachBuffer);
                int cap =  asset.getBufferCap();

                for(int k = 0; k < sortedParts.size(); k++){
                    String part = sortedParts.get(k);
                    Map<String, Pair<Double, Double>> thisMap = estAggMap.get(part);
                    Pair<Double, Double> tRange = thisMap.get(eachBuffer);
                    int curIndex = allParts.indexOf(part);

                    //If the buffer is the first, assign the range.right = afterPart.range.left
                    if(firstBuffer(eachBuffer,aggList)){
                        String nextBuf = findNext(eachBuffer, aggBuffers, hfm);
                        if (!nextBuf.isEmpty())
                        {
                            if(curIndex < allParts.size()-1)
                            {
                                //Get the next part
                                String afterPart = allParts.get(curIndex + 1);
                                Pair<Double, Double> aRange = estAggMap.get(afterPart).get(eachBuffer);

                                Pair<Double, Double> nRange = thisMap.get(nextBuf);

                                if(Double.compare(aRange.getLeft(), tRange.getLeft()) >= 0 && Double.compare(nRange.getRight(), aRange.getLeft()) >= 0)
                                {
                                    thisMap.put(eachBuffer,new MutablePair<>(tRange.getLeft(),aRange.getLeft()));
                                    thisMap.put(nextBuf, new MutablePair<>(aRange.getLeft(),nRange.getRight()));
                                }
                            }
                        }
                    }
                    //If the buffer is the last buffer, assign the .left = beforePart.right
                    else if(lastBuffer(eachBuffer,aggList)) {
                        if(curIndex >= 1)
                        {
                            String beforePart = allParts.get(curIndex - 1);
                            Pair<Double, Double> bRange = estAggMap.get(beforePart).get(eachBuffer);
                            String beforeBuf = findBefore(eachBuffer, aggBuffers, hfm);
                            if (!beforeBuf.isEmpty()) {

                                Pair<Double, Double> bfRange = thisMap.get(beforeBuf);
                                if (Double.compare(tRange.getRight(), bRange.getRight()) >= 0 && Double.compare(bRange.getRight(), bfRange.getLeft()) >= 0) {
                                    thisMap.put(eachBuffer, new MutablePair<>(bRange.getRight(), tRange.getRight()));
                                    thisMap.put(beforeBuf, new MutablePair<>(bfRange.getLeft(), bRange.getRight()));
                                }
                            }
                        }
                    }
                    //If the buffer is neither the last nor the first buffer, assign the .left = beforePart .right
                    else{
                        if(curIndex - cap >= 0)
                        {
                            String beforePart = allParts.get(curIndex - cap);

                            Pair<Double, Double> bRange = estAggMap.get(beforePart).get(eachBuffer);
                            String beforeBuf = findBefore(eachBuffer, aggBuffers, hfm);
                            if (!beforeBuf.isEmpty()) {

                                Pair<Double, Double> bfRange = thisMap.get(beforeBuf);
                                if (Double.compare(tRange.getRight(), bRange.getRight()) >= 0 && Double.compare(bRange.getRight(), bfRange.getLeft()) >= 0) {
                                    thisMap.put(eachBuffer, new MutablePair<>(bRange.getRight(), tRange.getRight()));
                                    thisMap.put(beforeBuf, new MutablePair<>(bfRange.getLeft(), bRange.getRight()));
                                }
                            }
                        }

        }
        System.out.println("Special - " + part + " " + estAggMap.get(part));
        }
        //}
        return estAggMap;
    }


    public static TreeMap<String, Map<String,Pair<Double, Double>>> Remove(TreeMap<String, Map<String,Pair<Double, Double>>> estAggMap,Map<Double,Map<String,Pair<HashSet<String>,Integer>>> outsideCaps,TreeMap<String, List<String>> aggList, List<String> aggBuffers,HFMs hfm){
        for (Double time : outsideCaps.keySet()) {
            Map<String, Pair<HashSet<String>, Integer>> bufferMap = outsideCaps.get(time);

            for (String eachBuffer : bufferMap.keySet()) {
                HashSet<String> parts = bufferMap.get(eachBuffer).getLeft();
                Integer dif = bufferMap.get(eachBuffer).getRight();
                List<Pair<String, Double>> partList = orderPartList(estAggMap);
                List<String> sortedParts = sortParts(parts, partList);
                //System.out.println("RemoveEst "+time + " - " + bufferMap);

                for (int i = 0; i < dif; i++) {
                    String firstPart = sortedParts.get(0);
                    Map<String, Pair<Double, Double>> fPart = estAggMap.get(firstPart);
                    Pair<Double, Double> fRange = fPart.get(eachBuffer);

                    if (Double.compare(time, fRange.getLeft()) > 0 && Double.compare(time, fRange.getRight()) <= 0) {
                        String secondPart = sortedParts.get(1);
                        Map<String, Pair<Double, Double>> sPart = estAggMap.get(secondPart);
                        Pair<Double, Double> sRange = sPart.get(eachBuffer);
                        //System.out.println("2nd " + secondPart + " " + sPart);
                        if (lastBuffer(eachBuffer, aggList)) {
                            String beforeBuf = findBefore(eachBuffer, aggBuffers, hfm);
                            if (!beforeBuf.isEmpty()) {
                                Pair<Double, Double> bRange = sPart.get(beforeBuf);
                                if(Double.compare(fRange.getRight(),bRange.getLeft()) > 0 && Double.compare(sRange.getRight(),fRange.getRight()) > 0){
                                    sPart.put(eachBuffer, new MutablePair<>(fRange.getRight(), sRange.getRight()));
                                    sPart.put(beforeBuf, new MutablePair<>(bRange.getLeft(), fRange.getRight()));
                                }
                            }
                            estAggMap.put(secondPart, sPart);
                            //System.out.println("2 - " + secondPart + " " + estAggMap.get(secondPart));
                        } else {  // if it is not the first buffer, change the time stamps for current part
                                fPart.put(eachBuffer, new MutablePair<>(fRange.getLeft(), time));
                                String nextBuf = findNext(eachBuffer, aggBuffers, hfm);
                                if (!nextBuf.isEmpty()) {
                                    Pair<Double, Double> nRange = fPart.get(nextBuf);
                                    if(Double.compare(time,fRange.getLeft()) > 0 && Double.compare(nRange.getRight(),time) > 0){
                                        fPart.put(eachBuffer, new MutablePair<>(fRange.getLeft(), time));
                                        fPart.put(nextBuf, new MutablePair<>(time, nRange.getRight()));
                                    }
                                }
                                estAggMap.put(firstPart, fPart);
                                //System.out.println("1 - " + firstPart + " " + estAggMap.get(firstPart));
                        }
                    }
                }
            }
        }
        return estAggMap;
    }

   public static TreeMap<String, Map<String,Pair<Double, Double>>> recAlgorithm(TreeMap<String, Map<String,Pair<Double, Double>>> estAggMap, TreeMap<String, List<String>> aggList, List<String> aggBuffers,HFMs hfm) {
       TreeMap<Double,Map<String,Pair<HashSet<String>,Integer>>> outsideCaps = testBufferCap(estAggMap,hfm);
        if(!outsideCaps.isEmpty()){

            Pair<Double,Map<String,HashSet<String>>> loop = LoopCheck(estAggMap,hfm,aggList,aggBuffers);
            Double time = loop.getLeft();
            TreeMap<String, Map<String,Pair<Double, Double>>> map = new TreeMap<>();
            if(Double.compare(time,0.0)>0){
                map = specialEst(estAggMap,loop,outsideCaps,aggList,aggBuffers,hfm);
            }
            else map = Remove(estAggMap,outsideCaps,aggList,aggBuffers,hfm);

            return recAlgorithm(map,aggList,aggBuffers,hfm);
       }
       else return estAggMap;
    }

    public static TreeMap<Double,Map<String,Pair<HashSet<String>,Integer>>> testBufferCap(TreeMap<String, Map<String,Pair<Double, Double>>> estAggMap,HFMs hfm) {
        TreeMap<Double,Map<String,HashSet<String>>> estMap = convertAll(estAggMap);
        TreeMap<Double,Map<String,Pair<HashSet<String>,Integer>>> output = new TreeMap<>();

        for (Double time : estMap.keySet())
        {
            Integer bufferCap;
            Map<String, HashSet<String>> bufferMap = estMap.get(time);
            for (String eachBuffer : bufferMap.keySet())
            {
                Asset buffer = hfm.getAssetsByID(eachBuffer);
                bufferCap = buffer.getBufferCap();
                //how many parts on the eachBuffer
                Integer real = bufferMap.get(eachBuffer).size();
                if (bufferCap < real) {
                    HashSet<String> parts = bufferMap.get(eachBuffer);
                    //System.err.println("At time " + time + ": " + eachBuffer + " hosting " + real + " " + parts + " > Cap " + bufferCap);
                    int dif = real - bufferCap;
                    Pair<HashSet<String>,Integer> pair = new ImmutablePair<>(parts,dif);
                    output.computeIfAbsent(time, v -> new TreeMap<>()).put(eachBuffer, pair);
                }
            }
        }

        return output;
    }

   public static List<Pair<String,Double>> orderPartList (TreeMap<String, Map<String,Pair<Double, Double>>> map){
        List<Pair<String,Double>> partList = new ArrayList<>();

        for(String eachPart: map.keySet()){
            Map<String,Pair<Double, Double>> value = map.get(eachPart);
            int i=0;
            double[] allTimes = new double[value.keySet().size()*2];
            for(String buffer: value.keySet()){
                Pair<Double,Double> timeRange = value.get(buffer);
                allTimes[i] = timeRange.getLeft();
                allTimes[i+1] = timeRange.getRight();
                i=i+2;
            }
            Arrays.sort(allTimes);
            partList.add(new MutablePair<>(eachPart,allTimes[0]));
        }
        partList.sort(Comparator.comparing(x -> x.getRight()));

        return partList;
    }

    public static List<String> sortParts (HashSet<String> parts,List<Pair<String,Double>> partList){
        List<String> ordered = new ArrayList<>(parts);
        /* Order the part list */
        ordered.sort((x, y) -> {
            Pair<String,Double> xPair = partList.stream().filter(a-> x.equals(a.getLeft())).findAny().orElse(null);
            Pair<String,Double> yPair = partList.stream().filter(b-> y.equals(b.getLeft())).findAny().orElse(null);
            return Double.compare(xPair.getRight(),yPair.getRight());
        });

        return ordered;
    }

    public static List<String> AllParts (List<Pair<String,Double>> partList){
        List<String> allParts = new ArrayList<>();
        partList.forEach(a -> allParts.add(a.getLeft()));
        return allParts;
    }

    public static String findNext (String buf, List<String> buffers, HFMs hfm){
        //List<String> sortedBufs = orderByConnectedTo(buffers,hfm);
        int index = buffers.indexOf(buf);
        String next = buffers.get(index+1);
        return next;
    }

    public static String findBefore (String buf, List<String> buffers, HFMs hfm){
        int index = buffers.indexOf(buf);
        String before = buffers.get(index-1);
        return before;
    }

    public static boolean firstBuffer (String buffer, TreeMap<String, List<String>> aggList){
        boolean output = false;
        for(String buf: aggList.keySet()){
            List<String> allBufs = aggList.get(buf);
            if(allBufs.get(0).equals(buffer)){
                output = true;
                break;
            }
        }
        return output;
    }

    public static boolean lastBuffer (String buffer, TreeMap<String, List<String>> aggList){
        boolean output = false;
        for(String buf: aggList.keySet()){
            List<String> allBufs = aggList.get(buf);
            int last = allBufs.size();
            if(last>0){
                if(allBufs.get(last-1).equals(buffer)){
                    output = true;
                    break;
                }
            }
        }
        return output;
    }

    public static <T> List<T> findDuplicate(List<T> list) {
        return new ArrayList<>(list.stream().filter(i -> Collections.frequency(list, i) > 1)
                .collect(Collectors.toSet()));
    }

}

