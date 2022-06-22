package Utils;

import java.util.ArrayList;
import java.util.List;


public interface Predicate<T> {
    boolean test(T t);

    static <T> List<T> filterList(List<T> list, Predicate<T> p) {
        List<T> result = new ArrayList<>();
        for(T e: list) {
            if(p.test(e)) {
                result.add(e);
            }
        }
        return result;
    }

    static <T> T filterSingle(List<T> list, Predicate<T> p) {
        T result = null;
        for(T e: list) {
            if(p.test(e)) {
                result = e;
            }
        }
        return result;
    }

    static <T> List<T> filterOutList(List<T> list, Predicate<T> p) {
        List<T> result = new ArrayList<>();
        for(T e: list) {
            if(!p.test(e)) {
                result.add(e);
            }
        }
        return result;
    }


}


