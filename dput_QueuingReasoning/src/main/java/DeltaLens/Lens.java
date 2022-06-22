package DeltaLens;

import java.util.function.BiFunction;
import java.util.function.Function;

public class Lens<A,B> {
    public final Function<A, B> getter;
    public final BiFunction<A, B, A> setter;

    public Lens(Function<A, B> getter, BiFunction<A, B, A> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public static <A, B> Lens<A, B> of(Function<A, B> getter, BiFunction<A, B, A> setter) {
        return new Lens<>(getter, setter);
    }

    public B get(A target) {
        return getter.apply(target);
    }

    public void set(A target, B value) {
        modify(ignore -> value).apply(target);
    }

    public Function<B, A> set(A target) {
        return (B b) -> modify(ignore -> b).apply(target);
    }

    /**
     * Modify B using a Function<B,B>, return the updated A
     * */
    public Function<A, A> modify(Function<B, B> mapper) {
        return (oldA) -> {
            B oldB = getter.apply(oldA);  // @param oldvalue: old A; @param extracted: old B
            B newB = mapper.apply(oldB); // @param transformed: new B; @function mapper is an update function on B
            return setter.apply(oldA, newB); //get and return the new A.
        };
    }

    public Function<Function<B, B>, A> modify(A oldValue) {
        return (mapper) -> {
            B extracted = getter.apply(oldValue);
            B transformed = mapper.apply(extracted);
            return setter.apply(oldValue, transformed);
        };
    }

    public <C> Lens<A, C> compose(Lens<B, C> other) {
        return new Lens<>(
                (A a) -> other.getter.apply(getter.apply(a)),
                (A a, C c) -> {
                    B b = getter.apply(a);
                    B newB = other.modify(ignored -> c).apply(b);
                    return setter.apply(a, newB);
                }
        );
    }
}
