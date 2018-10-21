package net.ttddyy.dsproxy.r2dbc.core;

import reactor.util.annotation.NonNull;

import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Comparator.naturalOrder;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public class Bindings {

    private SortedSet<Binding> indexBindings = new TreeSet<>();
    private SortedSet<Binding> identifierBindings = new TreeSet<>();

    public void addIndexBinding(int index, Object value) {
        this.indexBindings.add(new IndexBinding(index, value));
    }

    public void addIdentifierBinding(Object identifier, Object value) {
        this.identifierBindings.add(new IdentifierBinding(identifier, value));
    }

    public SortedSet<Binding> getIndexBindings() {
        return indexBindings;
    }

    public SortedSet<Binding> getIdentifierBindings() {
        return identifierBindings;
    }

    public static class IndexBinding implements
            Binding, Comparable<IndexBinding> {
        private int index;
        private Object value;

        public IndexBinding(int index, Object value) {
            this.index = index;
            this.value = value;
        }

        @Override
        public int compareTo(@NonNull IndexBinding o) {
            return Integer.compare(this.index, o.index);
        }

        @Override
        public Object getKey() {
            return this.index;
        }

        @Override
        public Object getValue() {
            return this.value;
        }
    }

    public static class IdentifierBinding implements
            Binding, Comparable<IdentifierBinding> {
        private Object identifier;
        private Object value;

        public IdentifierBinding(Object identifier, Object value) {
            this.identifier = identifier;
            this.value = value;
        }

        @Override
        public int compareTo(@NonNull IdentifierBinding o) {
            // TODO: implement
            return Objects.compare((Comparable) this.identifier,
                    (Comparable) o.identifier, naturalOrder());
        }

        @Override
        public Object getKey() {
            return this.identifier;
        }

        @Override
        public Object getValue() {
            return this.value;
        }

    }

}
