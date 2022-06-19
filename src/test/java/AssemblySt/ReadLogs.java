package AssemblySt;

/** This class read log files into different formats
 * Designed by Qunfen Qi
 * Date: 2021-01-12
 * */

//import Utils.Pair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReadLogs {

    /** Read the log file */
    public static TreeMap<String, Map<String,List<Pair<String, Double>>>> loadLog(String filePath)  {
        TreeMap<String, Map<String,List<Pair<String, Double>>>> logFile= new TreeMap<>();

        List<String> list = new ArrayList<>();
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            list = stream
                    .filter(line -> !line.startsWith("id"))
                    .collect(Collectors.toList()); } catch (IOException e) {
            e.printStackTrace();
        }
        list.forEach(
                s-> {
                    String[] line = s.split("\\s+");
                    String pallet = line[0];//pallet
                    try{
                        double time = Double.parseDouble(line[1]);// time stamp
                        String status = line[3];// enter or exist status
                        String buffer = line[4];// buffer
                        Pair<String, Double> pair = new MutablePair<>(status,time);
                        logFile.computeIfAbsent(pallet, v -> new TreeMap<>()).computeIfAbsent(buffer,a ->new ArrayList<>()).add(pair);
                    } catch(NumberFormatException ex){ // handle your exception
                    }
                }
        );
        return logFile;
    }

    /** Convert the output of logfile into a Map with parts as Key, and a pair of time stamps for each buffer */
    public static TreeMap<String, Map<String,Pair<Double, Double>>> converToPair(TreeMap<String, Map<String,List<Pair<String, Double>>>> logFile){
        TreeMap<String, Map<String,Pair<Double, Double>>> output= new TreeMap<>();
        for(String key:logFile.keySet()){
            Map<String,List<Pair<String, Double>>> bufferMap = logFile.get(key);
            Map<String, Pair<Double, Double>> bufferRange= new TreeMap<>();
            Double start = 0.0;
            Double end = 0.0;
            for(String eachBuffer : bufferMap.keySet()){
                //Double edge = 0.01;
                for(Pair<String,Double> each:bufferMap.get(eachBuffer)){
                    if(each.getLeft().equals("enter"))
                        start = each.getRight();
                    else if (each.getLeft().equals("exit"))
                        end = each.getRight();
                    //end = each.second-0.001;
                }
                bufferRange.put(eachBuffer,new MutablePair<>(start,end));
            }
            output.put(key,bufferRange);
        }
        return output;
    }

    /** Get all appeared buffers in the log file */
    public static HashSet<String> getBuffers(TreeMap<String, Map<String,Pair<Double, Double>>> logMap){
        HashSet<String> buffers = new HashSet<>();
        logMap.keySet().forEach(a -> buffers.addAll(logMap.get(a).keySet()));
        return buffers;
    }

}
