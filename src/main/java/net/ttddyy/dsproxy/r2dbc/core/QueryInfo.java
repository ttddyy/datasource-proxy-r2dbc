package net.ttddyy.dsproxy.r2dbc.core;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public class QueryInfo {

    private String query;

    private List<Bindings> bindingsList = new ArrayList<>();

    public QueryInfo() {
    }

    public QueryInfo(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    // TODO: improve
    public List<Bindings> getBindingsList() {
        return bindingsList;
    }
}
