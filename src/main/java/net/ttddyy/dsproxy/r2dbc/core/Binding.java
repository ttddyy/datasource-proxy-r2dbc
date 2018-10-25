package net.ttddyy.dsproxy.r2dbc.core;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public interface Binding {

    Object getKey();

    BindingValue getBindingValue();

}
