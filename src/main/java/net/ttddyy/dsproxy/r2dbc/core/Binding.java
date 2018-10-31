package net.ttddyy.dsproxy.r2dbc.core;

/**
 * Represent an operation of {@link io.r2dbc.spi.Statement#bind}.
 *
 * @author Tadaya Tsuyukubo
 * @see Bindings.IndexBinding
 * @see Bindings.IdentifierBinding
 */
public interface Binding {

    Object getKey();

    BindingValue getBindingValue();

}
