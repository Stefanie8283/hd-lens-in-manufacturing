package DeltaLens;

import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.PatchFailedException;

/**
 * Delta-Lens (D-Lens) zooming in to V (view) in S (source) via the record of changes of V (Diff)
 *
 * @param <S> source type
 * @param <V> view type
 * @param <diff> the record of actual (a collection of) changes on V
 */

abstract public class DLens<S,diff,V> {

    //get function - read the sub-structure, also can be used as the dget function: translate the source delta (SDiff) into a view delta Diff
    abstract public V get(S s);

    //Return a updated V via a Diff on V, similarly, you get updated S via a Diff on S.
    abstract public S update(S s, diff ds) throws PatchFailedException;

    //dput function: compute the source delta (SDiff) from view delta Diff
    abstract public diff dput(S s, V v, diff dv) throws PatchFailedException, DiffException;

    //put function via diff
    public S put(S s, V v, diff dv) throws PatchFailedException, DiffException {
            return this.update(s, this.dput(s, v, dv));
    }

    //put function via function
    /*public S put(S s, V v)  {
        return null;
    }*/

    /**
     * Update V using a Function<V,V>, return the updated S
     *
    public Function<V, V> modify(Function<V, V> mapper) {
        return (oldS) -> {
            V oldV = DLens.get(oldS);  // @param oldvalue: old V; @param extracted: old B
            V newV = mapper.apply(oldV); // @param transformed: new B; @function mapper is an update function on B
            return setter.apply(oldS, newV); //get and return the new A.
        };
    }*/

    //Return a copy of S with the substructure updated.
    //abstract public S update(S s, Diff ds) = S update(s, this.dput(s,v,dv));

    //Compose two Dlenses (S->V, and V->C) to get a DLens from S to C via computing the Diffs.
    public <C> DLens<S,diff,C> compose(final DLens<V, diff, C> DlensVC) {
        return new DLens<S, diff, C>() {
            @Override
            public C get(final S s) {
                return DlensVC.get(DLens.this.get(s));
            }

            @Override
            public S update(final S s, final diff ds) throws PatchFailedException{
                return DLens.this.update(s, ds);
            }

            @Override
            public diff dput(final S s, final C c, final diff dc) throws PatchFailedException, DiffException{
                return DLens.this.dput(s, DLens.this.get(s), DlensVC.dput(DLens.this.get(s), c, dc));
            }

            @Override
            public S put(final S s, final C c, final diff dc) throws PatchFailedException, DiffException{
                return DLens.this.put(s, DLens.this.get(s),DlensVC.dput(DLens.this.get(s),c, dc));
            }
        };
    }

}
