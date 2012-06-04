package litil.playground;

import java.util.*;

public class Exhaustivness {
    private interface Comb {
    }

    private static final class Id implements Comb {
        public final String name;

        private Id(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Id);
        }
    }

    private static final class Tuple implements Comb {
        public final List<Comb> combs;


        private Tuple(List<Comb> combs) {
            this.combs = combs;
        }

        private Tuple(Comb... combs) {
            this.combs = Arrays.asList(combs);
        }

        @Override
        public int hashCode() {
            return combs.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Tuple && combs.equals(((Tuple) o).combs);

        }
    }

    private static final class TyCon implements Comb {
        public final String name;
        public final List<Comb> combs;

        private TyCon(String name, List<Comb> combs) {
            this.name = name;
            this.combs = combs;
        }
    }

    private static List<List<Comb>> toExploreOfTyCons(List<Comb> combs) {
        Map<String, List<List<Comb>>> tyConsCombs = new HashMap<String, List<List<Comb>>>();
        for (Comb comb : combs) {
            TyCon tyCon = (TyCon) comb;
            List<List<Comb>> thisTyConCombs = tyConsCombs.get(tyCon.name);
            if (thisTyConCombs == null) {
                thisTyConCombs = new ArrayList<List<Comb>>();
                tyConsCombs.put(tyCon.name, thisTyConCombs);
            }
            thisTyConCombs.add(tyCon.combs);
        }


        List<List<Comb>> res = new ArrayList<List<Comb>>();

        for (List<List<Comb>> aTyConCombs : tyConsCombs.values()) {
            List<Comb> first = aTyConCombs.get(0);
            for (int i = 0, firstSize = first.size(); i < firstSize; i++) {
                List<Comb> toexplore = new ArrayList<Comb>();
                for (List<Comb> aTyConComb : aTyConCombs) {
                    toexplore.add(aTyConComb.get(i));
                }
                res.add(toexplore);

            }
        }
        return res;
    }

    private static List<List<Comb>> toExploreOfTuples(List<Comb> combs) {
        List<List<Comb>> res = new ArrayList<List<Comb>>();
        Tuple first = (Tuple) combs.get(0);
        List<Comb> combs1 = first.combs;
        for (int i = 0, combs1Size = combs1.size(); i < combs1Size; i++) {
            List<Comb> stageCombs = new ArrayList<Comb>();
            for (Comb comb : combs) {
                stageCombs.add(((Tuple) comb).combs.get(i));
            }
            res.add(stageCombs);
        }
        return res;
    }

    private static final List<String> CONS = Arrays.asList("A", "B");

    public List<String> missing(List<Comb> combs) {
        Comb first = combs.get(0);
        if (first instanceof TyCon) {

            List<String> handled = new ArrayList<String>(CONS);
            for (Comb comb : combs) {
                TyCon tyCon = (TyCon) comb;
                handled.remove(tyCon.name);

            }
            if (!handled.isEmpty()) {
                List<String> res = new ArrayList<String>();
                for (String s : handled) {
                    res.add("Unhandled tycon '" + s + "'");
                }
                return res;
            } else {
                List<String> res = new ArrayList<String>();
                List<List<Comb>> toexplore = toExploreOfTyCons(combs);
                for (List<Comb> combList : toexplore) {
                    res.addAll(missing(combList));
                }
                return res;
            }
        } else if (first instanceof Id) {
            return Collections.emptyList();
        } else if (first instanceof Tuple) {
            List<String> res = new ArrayList<String>();
            List<List<Comb>> toexplore = toExploreOfTuples(combs);
            for (List<Comb> combList : toexplore) {
                res.addAll(missing(combList));
            }
            return res;
        }

        throw new IllegalArgumentException("Unhandled comb type " + first);
    }

    public static void main(String[] args) {
        List<Comb> combs = tupleExample();
        System.out.println(new Exhaustivness().missing(combs));
    }

    private static List<Comb> tyConsExample() {
        List<Comb> combs = new ArrayList<Comb>();
        combs.add(new TyCon("A", Arrays.<Comb>asList(new TyCon("B", Collections.<Comb>emptyList()))));
        combs.add(new TyCon("A", Arrays.<Comb>asList(new TyCon("A", Collections.<Comb>emptyList()))));
        combs.add(new TyCon("B", Collections.<Comb>emptyList()));
        return combs;
    }

    private static List<Comb> tupleExample() {
        List<Comb> combs = new ArrayList<Comb>();
        TyCon ta = new TyCon("A", Collections.<Comb>emptyList());
        TyCon tb = new TyCon("B", Collections.<Comb>emptyList());
        combs.add(new Tuple(ta, tb));
        combs.add(new Tuple(tb, ta));
        return combs;
    }
}
