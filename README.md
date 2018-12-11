# datasource-proxy-r2dbc

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.ttddyy/datasource-proxy-r2dbc/badge.svg)][maven-central-badge]

[datasource-proxy][datasource-proxy] for [R2DBC][r2dbc-spi].

## About

Provide listeners that receive callbacks of query executions and method invocations on R2DBC SPI.

Callbacks are:
- Before/After query executions when `Batch#execute()` or `Statement#execute()` is called.
- Before/After any method calls on `ConnectionFactory`, `Connection`, `Batch`, `Statement` and `Result`. 
- Each mapped query result emitted by `Publisher<? extends Result>`.

Here is sample use cases for listeners:
- Query logging
- Slow query detection
- Tracing
- Metrics
- Assertion/Verification
  - Connection leak detection
  - Transaction check
- Custom logic injection
- etc.


## Use cases

### Query logging

When query is executed by `Batch#execute()` or `Statement#execute()`, listener receives query
callbacks.
The callback contains query execution information(`QueryExecutionInfo`) such as query string,
execution type, bindings, execution time, etc.  
You could output/log the information.

*Sample Output (wrapped for display purpose):*
```sql
# Statement with no bindings
# 
Thread:reactor-tcp-nio-1(30) Connection:1
Transaction:{Create:1 Rollback:0 Commit:0}
Success:True Time:34
Type:Statement BatchSize:0 BindingsSize:0
Query:["SELECT value FROM test"], Bindings:[]

# Batch query
#
Thread:reactor-tcp-nio-3(32) Connection:2
Transaction:{Create:1 Rollback:0 Commit:0}
Success:True Time:4
Type:Batch BatchSize:2 BindingsSize:0
Query:["INSERT INTO test VALUES(200)","SELECT value FROM test"], Bindings:[]

# Statement with multiple bindings
#
Thread:reactor-tcp-nio-1(30) Connection:3
Transaction:{Create:1 Rollback:0 Commit:0}
Success:True Time:21
Type:Statement BatchSize:0 BindingsSize:2
Query:["INSERT INTO test VALUES ($1,$2)"], Bindings:[(100,101),(200,null(int))]
```


### Slow query detection

There are two types of slow query detection.
- Detect slow query *AFTER* query has executed.
- Detect slow query *WHILE* query is running.

Former is simple. On `afterQuery` callback, check the execution time.
If it took more than threshold, perform an action such as logging, send notification, etc.

To perform some action _while_ query is still executing and it has passed the threshold time, one implementation
is to create a watcher that checks running queries and notify ones exceeded the threshold.  
It is currently in plan to port [`SlowQueryListener` from datasource-proxy](http://ttddyy.github.io/datasource-proxy/docs/current/user-guide/#_slow_query_logging_listener). 


### Method tracing

When any methods on proxy classes(`ConnectionFactory`, `Connection`, `Batch`, `Statement`, or `Result`)
are called, listeners receive callbacks on before and after invocations.

Below output simply printed out the method execution information(`MethodExecutionInfo`)
at each method invocation.  
Essentially, this shows interaction with R2DBC SPI. 

*Sample: Execution with transaction (see [sample](#sample)):*
```sql
  1: Thread:34 Connection:1 Time:16  PostgresqlConnectionFactory#create()
  2: Thread:34 Connection:1 Time:0  PostgresqlConnection#createStatement()
  3: Thread:34 Connection:1 Time:0  ExtendedQueryPostgresqlStatement#bind()
  4: Thread:34 Connection:1 Time:0  ExtendedQueryPostgresqlStatement#add()
  5: Thread:34 Connection:1 Time:5  PostgresqlConnection#beginTransaction()
  6: Thread:34 Connection:1 Time:5  ExtendedQueryPostgresqlStatement#execute()
  7: Thread:34 Connection:1 Time:3  PostgresqlConnection#commitTransaction()
  8: Thread:34 Connection:1 Time:4  PostgresqlConnection#close()
```

### Distributed Tracing

Sample project: [Tracing with sleuth](https://github.com/ttddyy/datasource-proxy-r2dbc-samples/tree/master/dsp-r2dbc-tracing-sleuth)

* [TracingExecutionListener](https://github.com/ttddyy/datasource-proxy-r2dbc-samples/blob/master/dsp-r2dbc-tracing-sleuth/src/main/java/net/ttddyy/TracingExecutionListener.java):
 Tracing listener implementation using `LifeCycleExecutionlistener`.


*Tracing*
![Tracing](https://github.com/ttddyy/datasource-proxy-r2dbc-samples/raw/master/dsp-r2dbc-tracing-sleuth/images/tracing-rollback.png)

*Connection Span*
![Connection Span](https://github.com/ttddyy/datasource-proxy-r2dbc-samples/raw/master/dsp-r2dbc-tracing-sleuth/images/span-connection.png)

*Query Span*
![Query Span](https://github.com/ttddyy/datasource-proxy-r2dbc-samples/raw/master/dsp-r2dbc-tracing-sleuth/images/span-batch-query.png)


### Metrics

On every callback, any obtained information can update metrics.

For example:
- Number of opened connections
- Number of rollbacks
- Method execution time 
- Number of queries
- Type of query (SELECT, DELETE, ...) 
- Query execution time
- etc.


Sample project: [Metrics with micrometer (and log slow queries)](https://github.com/ttddyy/datasource-proxy-r2dbc-samples/tree/master/dsp-r2dbc-metrics-micrometer)

This sample project populates following metrics:
- Time took to create a connection
- Commit and rollback counts
- Executed query count
- Slow query count

The key implementation is [MetricsExecutionListener](https://github.com/ttddyy/datasource-proxy-r2dbc-samples/blob/master/dsp-r2dbc-metrics-micrometer/src/main/java/net/ttddyy/MetricsExecutionListener.java)
which populates micrometer metrics and logs slow queries.

*Connection metrics on JMX*
![Connection JMX](https://github.com/ttddyy/datasource-proxy-r2dbc-samples/raw/master/dsp-r2dbc-metrics-micrometer/images/jmx-connection.png)

*Query metrics on JMX:*
![Query JMX](https://github.com/ttddyy/datasource-proxy-r2dbc-samples/raw/master/dsp-r2dbc-metrics-micrometer/images/jmx-query.png)

*Transaction metrics on actuator (`/actuator/metrics/r2dbc.transaction`):*
![Transaction Actuator](https://github.com/ttddyy/datasource-proxy-r2dbc-samples/raw/master/dsp-r2dbc-metrics-micrometer/images/actuator-transaction.png)



### Assertion/Verification

By inspecting invoked methods and/or executed queries, you can verify your logic has performed
as expected.

For example, by keeping track of connection open/close method calls, connection leaks can be
detected or verified.

Another example is to check group of the target queries are executed on the same connection.
This could verify the premise of transaction that queries need to be performed on the same
connection in order to be in the same transaction.
  

### Custom logic injection

Any logic can be performed on callbacks.
Thus, you can write own logic that performs anything, such as audit logging, sending
notifications, calling external system, etc.

## Samples

Sample projects: [datasource-proxy-r2dbc-samples](https://github.com/ttddyy/datasource-proxy-r2dbc-samples)

* [Tracing with Sleuth](https://github.com/ttddyy/datasource-proxy-r2dbc-samples/tree/master/dsp-r2dbc-tracing-sleuth)
  * [TracingExecutionListener](https://github.com/ttddyy/datasource-proxy-r2dbc-samples/blob/master/dsp-r2dbc-tracing-sleuth/src/main/java/net/ttddyy/TracingExecutionListener.java) implementation 

----

# API

## Listener API

`ProxyExecutionListener` is the foundation listener interface.
This defines callbacks for method invocation, query execution, and query result processing.

```java
// invoked before any method on proxy is called
void beforeMethod(MethodExecutionInfo executionInfo);

// invoked after any method on proxy is called
void afterMethod(MethodExecutionInfo executionInfo);

// invoked before query gets executed
void beforeQuery(QueryExecutionInfo execInfo);

// invoked after query is executed
void afterQuery(QueryExecutionInfo execInfo);

// invoked on processing(subscribing) each query result
void eachQueryResult(QueryExecutionInfo execInfo);
```

`MethodExecutionInfo` and `QueryExecutionInfo` contains contextual information about the
method/query execution.

Any method calls on proxied `ConnectionFactory`, `Connection`, `Batch`, `Statement`, and `Result`
triggers method callbacks - `beforeMethod()` and `afterMethod()`.  
`Batch#execute()` and `Statement#execute()` triggers query callbacks - `beforeQuery()`
and `afterQuery()`.(Specifically when returned result publisher is subscribed.)  
`eachQueryResult()` is called on each mapped query result when `Result#map()` is subscribed.


### LifeCycleListener

`LifeCycleListener` provides before/after methods for all methods defined on `ConnectionFactory`,
`Connection`, `Batch`, `Statement`, and `Result`, as well as method executions(`beforeMethod`, `afterMethod`),
query executions(`beforeQuery`, `afterQuery`) and result processing(`onEachQueryResult`).
This listener is built on top of method and query interceptor API on `ProxyExecutionListener`.

For example, if you want know the creation of connection and close of it:

```java
public class ConnectionStartToEndListener implements LifeCycleListener {

  @Override
  public void beforeCreateOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
    // callback at ConnectionFactory#create()
  }

  @Override
  public void afterCloseOnConnection(MethodExecutionInfo methodExecutionInfo) {
    // callback at Connection#close()
  }

}
```


## QueryExecutionInfoFormatter
This class converts `QueryExecutionInfo` to `String`. Mainly used for preparing log entries.  
Internally, this class has multiple consumers for `QueryExecutionInfo` and loop through them to
populate the output `StringBuilder`.  

This class implements `Function<QueryExecutionInfo,String>` and can be used in functional style as well.

```java
// convert all info
QueryExecutionInfoFormatter formatter = QueryExecutionInfoFormatter#showAll();
String str = formatter.format(queryExecutionInfo);

// customize conversion
QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
formatter.addConsumer((execInfo, sb) -> {
  sb.append("MY-QUERY-EXECUTION="); // add prefix
};
formatter.newLine();  // new line
formatter.showSuccess();
formatter.showConnection((execInfo, sb)  -> {
    // custom conversion
    sb.append("MY-ID=" + executionInfo.getConnectionInfo().getConnectionId());
});
formatter.showQuery();

// convert it
String str = formatter.format(queryExecutionInfo);
```

## MethodExecutionInfoFormatter

Similar to `QueryExecutionInfoFormatter`, `MethodExecutionInfoFormatter` converts `MethodExecutionInfo` to
`String`.

```java
MethodExecutionInfoFormatter formatter = MethodExecutionInfoFormatter.withDefault();

ProxyConnectionFactoryBuilder.create(connectionFactory)
  .onAfterMethod(execInfo ->
     execInfo.map(methodExecutionFormatter::format)  // convert
       .doOnNext(System.out::println)  // print out to sysout
       .subscribe())
  .build();
```


----

# Setup

Use `ProxyConnectionFactoryBuilder` to create a proxied `ConnectionFactory` and pass it to R2DBC client. 

```java
// original connection factory
ConnectionFactory connectionFactory = new PostgresqlConnectionFactory(configuration);

// create proxied connection factory
ConnectionFactory proxyConnectionFactory =
  ProxyConnectionFactoryBuilder.create(connectionFactory)  // pass original ConnectionFactory
    .onAfterMethod(mono -> {
      ...   // callback after method execution
    })
    .onEachQueryResult(mono -> {
      ...   // callback for each mapped result 
    })
    .onAfterQuery(mono -> {
      ...  //  callback after query execution
    })
    .build();

// initialize client with the wrappd connection factory
R2dbc client = new R2dbc(proxyConnectionFactory);
```

## Install

- local maven install
  ```shell
  ./mvnw install
  ```

- [jitpack][jitpack]


## Versions

datasource-proxy-r2dbc is developed on following versions.

| datasource-proxy-r2dbc |       r2dbc-spi      |   reactor-core  |
|:----------------------:|:--------------------:|:---------------:|
| 0.2-SNAPSHOT           | 1.0.0.M6             | Californium-SR2 |
| 0.1                    | 1.0.0.M6             | Californium-SR2 |

*NOTE:*  
Currently, it is built on milestone release of r2dbc-spi.  
To get milestone releases, spring-milestones repo needs to be added.

```xml
<repositories>
  <repository>
    <id>spring-milestones</id>
    <name>Spring Milestones</name>
    <url>https://repo.spring.io/milestone</url>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
  </repository>
</repositories>
```


## Maven and Gradle

*Maven*
```xml
<dependency>
  <groupId>net.ttddyy</groupId>
  <artifactId>datasource-proxy-r2dbc</artifactId>
  <version>${latest-version}</version>
</dependency>
```

*Gradle*
```groovy
compile "net.ttddyy:datasource-proxy-r2dbc:${latest-version}"
```

NOTE: `artifactId` may change in future.


----

# Sample Configuration

## Query logging

On after query callback, write out executed query information.
This can be done in *before* query execution; however, some of the attributes are only
available at *after* query execution such as execution time, successfully executed, etc.

`QueryExecutionInfoFormatter`, which converts `QueryExecutionInfo` to `String`, can be used
out of the box to generate log statements.

```java
QueryExecutionInfoFormatter queryExecutionFormatter = QueryExecutionInfoFormatter.showAll();

ConnectionFactory proxyConnectionFactory =
  ProxyConnectionFactoryBuilder.create(connectionFactory)  // wrap original ConnectionFactory
    // on every query execution
    .onAfterQuery(execInfo ->
      execInfo.map(queryExecutionFormatter::format)    // convert QueryExecutionInfo to String
              .doOnNext(System.out::println)       // print out executed query
              .subscribe())
    .build();
```

## Slow query detection

### Detect slow query AFTER query has executed

On after query execution, check whether the query execution time has exceeded the threshold
time, then perform any action.  

```java
Duration threshold = Duration.of(...);

ConnectionFactory proxyConnectionFactory =
  ProxyConnectionFactoryBuilder.create(connectionFactory)
    .onAfterQuery(mono -> mono
       .filter(execInfo -> threshold.minus(execInfo.getExecuteDuration()).isNegative())
       .doOnNext(execInfo -> {
         // slow query logic
       })
       .subscribe())
    .build();
```

### Detect slow query WHILE query is executing

TBD for slow query detection _while_ query is executing.


## Method tracing

At each invocation of methods, perform action such as printing out the invoked method,
create a span, or update metrics.

`MethodExecutionInfoFormatter` is used to generate log string.

```java
MethodExecutionInfoFormatter methodExecutionFormatter = MethodExecutionInfoFormatter.withDefault();

ConnectionFactory proxyConnectionFactory =
  ProxyConnectionFactoryBuilder.create(connectionFactory)  // wrap original ConnectionFactory
    // on every method invocation
    .onAfterMethod(execInfo ->  
      execInfo.map(methodExecutionFormatter::format)    // convert MethodExecutionInfo to String
              .doOnNext(System.out::println)        // print out method execution (method tracing)
              .subscribe())
    .build();
```

----

## Sample

Client code:
```java
// Simple Transaction Example
getR2dbc()
  .withHandle(handle -> handle
    .inTransaction(h -> h.execute("INSERT INTO test VALUES ($1)", 200)))
  .subscribe();
```

Setup:
```java
// converter: from execution info to String
QueryExecutionInfoFormatter queryExecutionFormatter = QueryExecutionInfoFormatter.showAll();
MethodExecutionInfoFormatter methodExecutionFormatter = MethodExecutionInfoFormatter.withDefault();

ConnectionFactory proxyConnectionFactory =
  ProxyConnectionFactoryBuilder.create(connectionFactory)  // wrap original ConnectionFactory
    // on every method invocation
    .onAfterMethod(execInfo ->  
      execInfo.map(methodExecutionFormatter::format)
              .doOnNext(System.out::println)        // print out method execution (method tracing)
              .subscribe())
    // on every query execution
    .onAfterQuery(execInfo ->
      execInfo.map(queryExecutionFormatter::format)
              .doOnNext(System.out::println)       // print out executed query
              .subscribe())
    .build();

// pass the proxied ConnectionFactory to client
this.r2dbc = new R2dbc(proxyConnectionFactory);
```

Method tracing output:
```sql
  1: Thread:34 Connection:1 Time:16  PostgresqlConnectionFactory#create()
  2: Thread:34 Connection:1 Time:0  PostgresqlConnection#createStatement()
  3: Thread:34 Connection:1 Time:0  ExtendedQueryPostgresqlStatement#bind()
  4: Thread:34 Connection:1 Time:0  ExtendedQueryPostgresqlStatement#add()
  5: Thread:34 Connection:1 Time:5  PostgresqlConnection#beginTransaction()
  6: Thread:34 Connection:1 Time:5  ExtendedQueryPostgresqlStatement#execute()
  7: Thread:34 Connection:1 Time:3  PostgresqlConnection#commitTransaction()
  8: Thread:34 Connection:1 Time:4  PostgresqlConnection#close()
```

Query output: (wrapped for display)
```sql
Thread:reactor-tcp-nio-1(30) Connection:1
Transaction:{Create:1 Rollback:0 Commit:0}
Success:True Time:32
Type:Statement BatchSize:0 BindingsSize:1 
Query:["INSERT INTO test VALUES ($1)"] Bindings:[(200)]
```

----

[maven-central-badge]: https://maven-badges.herokuapp.com/maven-central/net.ttddyy/datasource-proxy-r2dbc/ 
[datasource-proxy]: https://github.com/ttddyy/datasource-proxy
[jitpack]: https://jitpack.io/#ttddyy/datasource-proxy-r2dbc/
[r2dbc-spi]: https://github.com/r2dbc/r2dbc-spi 
