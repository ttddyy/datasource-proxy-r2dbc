# datasource-proxy-r2dbc

[datasource-proxy][datasource-proxy] for [R2DBC][r2dbc-spi].

## About

Create proxies for R2DBC SPI, and provides listeners for query executions and method invocations.

Registered listeners receive callbacks:
- before/after query executions
- before/after any method calls on `Connection`, `Batch` and `Statement` 


## Setup

Wrap original `ConnectionFactory` by `ProxyConnectionFactory` and pass it to R2DBC client.

```java
// original connection factory
ConnectionFactory connectionFactory = new PostgresqlConnectionFactory(configuration);

// create proxied connection factory
ConnectionFactory proxyConnectionFactory =
  ProxyConnectionFactory.create(connectionFactory)  // wrap original ConnectionFactory
    .onAfterMethod(mono -> {
      ...   // callback after method execution
    })  
    .onAfterQuery(mono -> {
      ...  //  callback after query execution
    });

// initialize client with the wrappd connection factory
R2dbc client = new R2dbc(proxyConnectionFactory);
```


## Usage example

- Query logging
- Slow query detection
- Method tracing
- Metrics
- Assertion/Verification
  - Connection leak detection
  - Transaction check
- Custom logic injection
- etc.


### Query logging

Listener is called when query is executed by `Batch#execute()` or `Statement#execute()`.
The callback contains query execution information(`QueryExecutionInfo`) such as query string,
bindings, type, execution time, etc.
You could output/log the information.

*Sample Output (wrapped for display purpose):*
```sql
# Statement with no bindings
# 
Thread:reactor-tcp-nio-1(30) Connection:1 Success:True Time:34
Type:Statement BatchSize:0 BindingsSize:0 
Query:["SELECT value FROM test"], Bindings:[]

# Batch query
#
Thread:reactor-tcp-nio-3(32) Connection:2 Success:True Time:4
Type:Batch BatchSize:2 BindingsSize:0
Query:["INSERT INTO test VALUES(200)","SELECT value FROM test"], Bindings:[]

# Statement with multiple bindings
#
Thread:reactor-tcp-nio-1(30) Connection:3 Success:True Time:21
Type:Statement BatchSize:0 BindingsSize:2
Query:["INSERT INTO test VALUES ($1)"], Bindings:[(100),(200)]
```

`QueryExecutionInfoFormatter`, which converts `QueryExecutionInfo` to `String`, can be used
out of the box to generate log statements.


### Slow query detection

There are two types of slow query detection.
- Detect slow query *AFTER* query has executed.
- Detect slow query *WHILE* query is running.

Former is simple. On `afterQuery` callback, check the execution time.
If it took more than threashold, perform an action such as logging, send notification, etc. 

To perform action _while_ query is still executing and passed threashold time, one implementation
is to create a watcher that checks running queries and notify ones exceeded the threshold.

Planning to port [`SlowQueryListener` from datasource-proxy](http://ttddyy.github.io/datasource-proxy/docs/current/user-guide/#_slow_query_logging_listener). 


*Sample: Perform slow query after execution*
```java
Duration threashold = Duration.of(...);

ConnectionFactory proxyConnectionFactory =
  ProxyConnectionFactory.create(connectionFactory)
    .onAfterQuery(mono -> mono
       .filter(execInfo -> threashold.minus(execInfo.getExecuteDuration()).isNegative())
       .doOnNext(execInfo -> {
         // slow query logic
       })
       .subscribe());
```

TBD for slow query detection _while_ query is executing.


### Method tracing

When any methods on `Connection`, `Batch`, or `Statement` are called,
listeners receive callbacks on before and after invocations.

Below output simply printed out the method execution information(`MethodExecutionInfo`)
at each method invocation.  
Essentially, this shows interaction with R2DBC SPI. 

You could even call distributed tracing system to record events.

*Sample: Execution with transaction (see [sample](#sample)):*
```sql
  1: Thread:0 Connection:1 Time:3  PostgresqlConnection#createStatement()
  2: Thread:0 Connection:1 Time:4  ExtendedQueryPostgresqlStatement#bind()
  3: Thread:0 Connection:1 Time:0  ExtendedQueryPostgresqlStatement#add()
  4: Thread:30 Connection:1 Time:13  PostgresqlConnection#beginTransaction()
  5: Thread:30 Connection:1 Time:34  ExtendedQueryPostgresqlStatement#execute()
  6: Thread:30 Connection:1 Time:6  PostgresqlConnection#commitTransaction()
  7: Thread:30 Connection:1 Time:6  PostgresqlConnection#close()
```

`MethodExecutionInfoFormatter` is used to generate above log statement.

### Metrics

On every callback, any information can update metrics.

For method execution callbacks, number of opened connection, method execution time that
took more than X, etc.; For query execution callbacks, number of queries, type of query(SELECT, DELETE, ...), 
execution time, etc. can be used for metrics. 


### Assertion/Verification

By keeping invoked methods and/or executed queries, you can perform assertion/verification
to see target logic has performed appropriately.

For example, by keeping track of connection open/close method calls, connection leaks can be
detected or verified.

Another example is to check target queries are executed on the same connection.
This could verify the premise of transaction.
  

### Custom logic injection

Any logic can be performed at callback.
Thus, you can write logic that performs audit log, send notification, call external system, etc.


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
  ProxyConnectionFactory.create(connectionFactory)  // wrap original ConnectionFactory
    // on every method invocation
    .onAfterMethod(execInfo ->  
      execInfo.map(methodExecutionFormatter::format)
              .doOnNext(System.out::println)        // print out method execution (method tracing)
              .subscribe())
    // on every query execution
    .onAfterQuery(execInfo ->
      execInfo.map(queryExecutionFormatter::format)
              .doOnNext(System.out::println)       // print out executed query
              .subscribe());

// pass the proxied ConnectionFactory to client
this.r2dbc = new R2dbc(proxyConnectionFactory);
```

Method tracing output:
```sql
  1: Thread:0 Connection:1 Time:3  PostgresqlConnection#createStatement()
  2: Thread:0 Connection:1 Time:4  ExtendedQueryPostgresqlStatement#bind()
  3: Thread:0 Connection:1 Time:0  ExtendedQueryPostgresqlStatement#add()
  4: Thread:30 Connection:1 Time:13  PostgresqlConnection#beginTransaction()
  5: Thread:30 Connection:1 Time:34  ExtendedQueryPostgresqlStatement#execute()
  6: Thread:30 Connection:1 Time:6  PostgresqlConnection#commitTransaction()
  7: Thread:30 Connection:1 Time:6  PostgresqlConnection#close()
```

Query output: (wrapped for display)
```sql
Thread:reactor-tcp-nio-1(30) Connection:1 Success:True Time:32 
Type:Statement BatchSize:0 BindingsSize:1 
Query:["INSERT INTO test VALUES ($1)"] Bindings:[(200)]
```

----

## Install

- local maven install
  ```shell
  ./mvnw install
  ```

- [jitpack][jitpack]


## Versions

Developed on following versions.

| datasource-proxy-r2dbc |       r2dbc-spi      |   reactor-core  |
|:----------------------:|:--------------------:|:---------------:|
| 0.1-SNAPSHOT           | 1.0.0.BUILD-SNAPSHOT | Californium-SR1 |

----

[datasource-proxy]: https://github.com/ttddyy/datasource-proxy
[jitpack]: https://jitpack.io/#ttddyy/datasource-proxy-r2dbc/
[r2dbc-spi]: https://github.com/r2dbc/r2dbc-spi 

