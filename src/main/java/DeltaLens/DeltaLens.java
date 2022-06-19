package DeltaLens;

import com.github.difflib.patch.AbstractDelta;

import java.util.function.BiFunction;
import java.util.function.Function;

public class DeltaLens <S,V>  extends Lens<S,V> {

    public DeltaLens(Function<S, V> get, BiFunction<S, V, S> put) {
        super(get, put);
    }

    //Defining a new put function: update V using a delta instead of a Function, return the updated S
    public Function<S, S> put(AbstractDelta deltaV) {
        return (oldS) -> {
            V newV = (V) deltaV.getTarget().getLines();
            System.out.println("newV: "+newV);
            //return null;
            return setter.apply(oldS,newV);
        };
    }

    /**
     * Modify B using a delta>, return the updated A
     *
    public Function<A, A> modify(Function<B, B> mapper) {
        return (oldA) -> {
            B oldB = getter.apply(oldA);  // @param oldvalue: old A; @param extracted: old B
            B newB = mapper.apply(oldB); // @param transformed: new B; @function mapper is an update function on B
            return setter.apply(oldA, newB); //get and return the new A.
        };
    }*/


    public static <A, B> DeltaLens<A, B> of(Function<A, B> getter, BiFunction<A, B, A> setter) {
        return new DeltaLens<>(getter, setter);
    }

    /*
     */

}
