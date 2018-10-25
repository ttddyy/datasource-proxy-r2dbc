package net.ttddyy.dsproxy.r2dbc;

import net.ttddyy.dsproxy.r2dbc.core.Bindings;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ExecutionType;
import net.ttddyy.dsproxy.r2dbc.core.QueryInfo;
import net.ttddyy.dsproxy.r2dbc.core.BindingValue.NullBindingValue;
import net.ttddyy.dsproxy.r2dbc.core.BindingValue.SimpleBindingValue;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Tadaya Tsuyukubo
 */
public class QueryExecutionInfoFormatterTest {

    @Test
    void batchExecution() {

        // Batch Query
        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setThreadName("my-thread");
        execInfo.setThreadId(99);
        execInfo.setConnectionId("conn-id");
        execInfo.setSuccess(true);
        execInfo.setExecuteDuration(Duration.of(35, ChronoUnit.MILLIS));
        execInfo.setType(ExecutionType.BATCH);
        execInfo.setBatchSize(20);
        execInfo.setBindingsSize(10);
        execInfo.getQueries().addAll(Arrays.asList(new QueryInfo("SELECT A"), new QueryInfo("SELECT B")));

        ExecutionInfoFormatter formatter = ExecutionInfoFormatter.showAll();

        String result = formatter.format(execInfo);
        assertEquals("Thread:my-thread(99) Connection:conn-id Success:True Time:35 " +
                "Type:Batch BatchSize:20 BindingsSize:10 " +
                "Query:[\"SELECT A\",\"SELECT B\"] Bindings:[]", result);

    }

    @Test
    void statementExecution() {

        Bindings indexBindings1 = new Bindings();
        indexBindings1.addIndexBinding(0, new SimpleBindingValue("100"));
        indexBindings1.addIndexBinding(1, new SimpleBindingValue("101"));
        indexBindings1.addIndexBinding(2, new SimpleBindingValue("102"));

        Bindings indexBindings2 = new Bindings();
        indexBindings2.addIndexBinding(2, new SimpleBindingValue("202"));
        indexBindings2.addIndexBinding(1, new NullBindingValue(String.class));
        indexBindings2.addIndexBinding(0, new SimpleBindingValue("200"));


        Bindings idBindings1 = new Bindings();
        idBindings1.addIdentifierBinding("$0", new SimpleBindingValue("100"));
        idBindings1.addIdentifierBinding("$1", new SimpleBindingValue("101"));
        idBindings1.addIdentifierBinding("$2", new SimpleBindingValue("102"));

        Bindings idBindings2 = new Bindings();
        idBindings2.addIdentifierBinding("$2", new SimpleBindingValue("202"));
        idBindings2.addIdentifierBinding("$1", new NullBindingValue(Integer.class));
        idBindings2.addIdentifierBinding("$0", new SimpleBindingValue("200"));

        QueryInfo queryWithIndexBindings = new QueryInfo("SELECT WITH-INDEX");
        QueryInfo queryWithIdBindings = new QueryInfo("SELECT WITH-IDENTIFIER");
        QueryInfo queryWithNoBindings = new QueryInfo("SELECT NO-BINDINGS");

        queryWithIndexBindings.getBindingsList().addAll(Arrays.asList(indexBindings1, indexBindings2));
        queryWithIdBindings.getBindingsList().addAll(Arrays.asList(idBindings1, idBindings2));

        // Statement Query
        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setThreadName("my-thread");
        execInfo.setThreadId(99);
        execInfo.setConnectionId("conn-id");
        execInfo.setSuccess(true);
        execInfo.setExecuteDuration(Duration.of(35, ChronoUnit.MILLIS));
        execInfo.setType(ExecutionType.STATEMENT);
        execInfo.setBatchSize(20);
        execInfo.setBindingsSize(10);


        ExecutionInfoFormatter formatter = ExecutionInfoFormatter.showAll();
        String result;

        // with index bindings
        execInfo.setQueries(Collections.singletonList(queryWithIndexBindings));
        result = formatter.format(execInfo);
        assertEquals("Thread:my-thread(99) Connection:conn-id Success:True Time:35" +
                " Type:Statement BatchSize:20 BindingsSize:10 Query:[\"SELECT WITH-INDEX\"]" +
                " Bindings:[(100,101,102),(200,null(String),202)]", result);

        // with identifier bindings
        execInfo.setQueries(Collections.singletonList(queryWithIdBindings));
        result = formatter.format(execInfo);
        assertEquals("Thread:my-thread(99) Connection:conn-id Success:True Time:35" +
                " Type:Statement BatchSize:20 BindingsSize:10 Query:[\"SELECT WITH-IDENTIFIER\"]" +
                " Bindings:[($0=100,$1=101,$2=102),($0=200,$1=null(Integer),$2=202)]", result);

        // with no bindings
        execInfo.setQueries(Collections.singletonList(queryWithNoBindings));
        result = formatter.format(execInfo);
        assertEquals("Thread:my-thread(99) Connection:conn-id Success:True Time:35" +
                " Type:Statement BatchSize:20 BindingsSize:10 Query:[\"SELECT NO-BINDINGS\"]" +
                " Bindings:[]", result);

    }


    @Test
    void onThread() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.addConsumer(ExecutionInfoFormatter.DEFAULT_ON_THREAD);

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setThreadName("my-thread");
        execInfo.setThreadId(99);

        String str = formatter.format(execInfo);
        assertEquals("Thread:my-thread(99)", str);
    }

    @Test
    void onConnection() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.addConsumer(ExecutionInfoFormatter.DEFAULT_ON_CONNECTION);

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setConnectionId("99");

        String str = formatter.format(execInfo);
        assertEquals("Connection:99", str);
    }

    @Test
    void onSuccess() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.addConsumer(ExecutionInfoFormatter.DEFAULT_ON_SUCCESS);

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setSuccess(true);

        String str = formatter.format(execInfo);
        assertEquals("Success:True", str);

        execInfo.setSuccess(false);

        str = formatter.format(execInfo);
        assertEquals("Success:False", str);
    }

    @Test
    void onTime() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.addConsumer(ExecutionInfoFormatter.DEFAULT_ON_TIME);

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setExecuteDuration(Duration.of(55, ChronoUnit.MILLIS));

        String str = formatter.format(execInfo);
        assertEquals("Time:55", str);
    }

    @Test
    void onType() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.addConsumer(ExecutionInfoFormatter.DEFAULT_ON_TYPE);

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        String str;

        execInfo.setType(ExecutionType.BATCH);
        str = formatter.format(execInfo);
        assertEquals("Type:Batch", str);

        execInfo.setType(ExecutionType.STATEMENT);
        str = formatter.format(execInfo);
        assertEquals("Type:Statement", str);
    }

    @Test
    void onBatchSize() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.addConsumer(ExecutionInfoFormatter.DEFAULT_ON_BATCH_SIZE);

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setBatchSize(99);

        String str = formatter.format(execInfo);
        assertEquals("BatchSize:99", str);
    }

    @Test
    void onBindingsSize() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.addConsumer(ExecutionInfoFormatter.DEFAULT_ON_BINDINGS_SIZE);

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setBindingsSize(99);

        String str = formatter.format(execInfo);
        assertEquals("BindingsSize:99", str);
    }

    @Test
    void onQuery() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.addConsumer(ExecutionInfoFormatter.DEFAULT_ON_QUERY);

        QueryInfo query1 = new QueryInfo("QUERY-1");
        QueryInfo query2 = new QueryInfo("QUERY-2");
        QueryInfo query3 = new QueryInfo("QUERY-3");

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        String result;


        // with multiple bindings
        execInfo.setQueries(Arrays.asList(query1, query2, query3));
        result = formatter.format(execInfo);
        assertEquals("Query:[\"QUERY-1\",\"QUERY-2\",\"QUERY-3\"]", result);

        // with single bindings
        execInfo.setQueries(Collections.singletonList(query2));
        result = formatter.format(execInfo);
        assertEquals("Query:[\"QUERY-2\"]", result);

        // with no bindings
        execInfo.setQueries(Collections.emptyList());
        result = formatter.format(execInfo);
        assertEquals("Query:[]", result);

    }

    @Test
    void onBindingsWithIndexBinding() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.addConsumer(ExecutionInfoFormatter.DEFAULT_ON_BINDINGS);

        Bindings bindings1 = new Bindings();
        bindings1.addIndexBinding(0, new SimpleBindingValue("100"));
        bindings1.addIndexBinding(1, new SimpleBindingValue("101"));
        bindings1.addIndexBinding(2, new SimpleBindingValue("102"));

        Bindings bindings2 = new Bindings();
        bindings2.addIndexBinding(2, new SimpleBindingValue("202"));
        bindings2.addIndexBinding(1, new NullBindingValue(String.class));
        bindings2.addIndexBinding(0, new SimpleBindingValue("200"));

        Bindings bindings3 = new Bindings();
        bindings3.addIndexBinding(1, new SimpleBindingValue("300"));
        bindings3.addIndexBinding(2, new SimpleBindingValue("302"));
        bindings3.addIndexBinding(0, new NullBindingValue(Integer.class));

        QueryInfo query1 = new QueryInfo();  // will have 3 bindings
        QueryInfo query2 = new QueryInfo();  // will have 1 bindings
        QueryInfo query3 = new QueryInfo();  // will have empty bindings

        query1.getBindingsList().addAll(Arrays.asList(bindings1, bindings2, bindings3));
        query2.getBindingsList().addAll(Arrays.asList(bindings2));


        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        String result;


        // with multiple bindings
        execInfo.setQueries(Collections.singletonList(query1));
        result = formatter.format(execInfo);
        assertEquals("Bindings:[(100,101,102),(200,null(String),202),(null(Integer),300,302)]", result);

        // with single bindings
        execInfo.setQueries(Collections.singletonList(query2));
        result = formatter.format(execInfo);
        assertEquals("Bindings:[(200,null(String),202)]", result);

        // with no bindings
        execInfo.setQueries(Collections.singletonList(query3));
        result = formatter.format(execInfo);
        assertEquals("Bindings:[]", result);

    }

    @Test
    void onBindingsWithIdentifierBinding() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.addConsumer(ExecutionInfoFormatter.DEFAULT_ON_BINDINGS);

        Bindings bindings1 = new Bindings();
        bindings1.addIdentifierBinding("$0", new SimpleBindingValue("100"));
        bindings1.addIdentifierBinding("$1", new SimpleBindingValue("101"));
        bindings1.addIdentifierBinding("$2", new SimpleBindingValue("102"));

        Bindings bindings2 = new Bindings();
        bindings2.addIdentifierBinding("$2", new SimpleBindingValue("202"));
        bindings2.addIdentifierBinding("$1", new NullBindingValue(Long.class));
        bindings2.addIdentifierBinding("$0", new SimpleBindingValue("200"));

        Bindings bindings3 = new Bindings();
        bindings3.addIdentifierBinding("$1", new SimpleBindingValue("300"));
        bindings3.addIdentifierBinding("$2", new SimpleBindingValue("302"));
        bindings3.addIdentifierBinding("$0", new NullBindingValue(String.class));

        QueryInfo query1 = new QueryInfo();  // will have 3 bindings
        QueryInfo query2 = new QueryInfo();  // will have 1 bindings
        QueryInfo query3 = new QueryInfo();  // will have empty bindings

        query1.getBindingsList().addAll(Arrays.asList(bindings1, bindings2, bindings3));
        query2.getBindingsList().addAll(Arrays.asList(bindings2));


        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        String result;


        // with multiple bindings
        execInfo.setQueries(Collections.singletonList(query1));
        result = formatter.format(execInfo);
        assertEquals("Bindings:[($0=100,$1=101,$2=102),($0=200,$1=null(Long),$2=202),($0=null(String),$1=300,$2=302)]", result);

        // with single bindings
        execInfo.setQueries(Collections.singletonList(query2));
        result = formatter.format(execInfo);
        assertEquals("Bindings:[($0=200,$1=null(Long),$2=202)]", result);

        // with no bindings
        execInfo.setQueries(Collections.singletonList(query3));
        result = formatter.format(execInfo);
        assertEquals("Bindings:[]", result);

    }

    @Test
    void showAll() {
        ExecutionInfoFormatter formatter = ExecutionInfoFormatter.showAll();
        String result = formatter.format(new QueryExecutionInfo());
        assertThat(result)
                .containsSubsequence("Thread", "Connection", "Success", "Time", "Type", "BatchSize",
                        "BindingsSize", "Query", "Bindings");
    }

    @Test
    void defaultInstance() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("", result, "Does not generate anything.");
    }

    @Test
    void showThread() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showThread();
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("Thread:(0)", result);
    }

    @Test
    void showThreadWithConsumer() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showThread(((executionInfo, sb) -> sb.append("my-thread")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-thread", result);
    }


    @Test
    void showConnection() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showConnection();
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("Connection:", result);
    }

    @Test
    void showConnectionWithConsumer() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showConnection(((executionInfo, sb) -> sb.append("my-connection")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-connection", result);
    }

    @Test
    void showSuccess() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showSuccess();
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("Success:False", result);
    }

    @Test
    void showSuccessWithConsumer() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showSuccess(((executionInfo, sb) -> sb.append("my-success")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-success", result);
    }

    @Test
    void showTime() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showTime();
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("Time:0", result);
    }

    @Test
    void showTimeWithConsumer() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showTime(((executionInfo, sb) -> sb.append("my-time")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-time", result);
    }

    @Test
    void showType() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showType();
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("Type:Statement", result);
    }

    @Test
    void showTypeWithConsumer() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showType(((executionInfo, sb) -> sb.append("my-type")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-type", result);
    }

    @Test
    void showBatchSize() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showBatchSize();
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("BatchSize:0", result);
    }

    @Test
    void showBatchSizeWithConsumer() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showBatchSize(((executionInfo, sb) -> sb.append("my-batchsize")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-batchsize", result);
    }

    @Test
    void showBindingsSize() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showBindingsSize();
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("BindingsSize:0", result);
    }

    @Test
    void showBindingsSizeWithConsumer() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showBindingsSize(((executionInfo, sb) -> sb.append("my-bindingsize")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-bindingsize", result);
    }

    @Test
    void showQuery() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showQuery();
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("Query:[]", result);
    }

    @Test
    void showQueryWithConsumer() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showQuery(((executionInfo, sb) -> sb.append("my-query")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-query", result);
    }

    @Test
    void showBindings() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showBindings();
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("Bindings:[]", result);
    }

    @Test
    void showBindingsWithConsumer() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.showBindings(((executionInfo, sb) -> sb.append("my-binding")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-binding", result);
    }

    @Test
    void delimiter() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter()
                .showTime()
                .showSuccess()
                .delimiter("ZZZ");

        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("Time:0ZZZSuccess:False", result);
    }

    @Test
    void newLine() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter()
                .showTime()
                .newLine()
                .showSuccess()
                .newLine()
                .showBatchSize();

        String lineSeparator = System.lineSeparator();
        String expected = format("Time:0 %sSuccess:False %sBatchSize:0", lineSeparator, lineSeparator);

        String result = formatter.format(new QueryExecutionInfo());
        assertEquals(expected, result);
    }

}
