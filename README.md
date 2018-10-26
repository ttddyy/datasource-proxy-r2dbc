# datasource-proxy-r2dbc

[datasource-proxy][datasource-proxy] for [R2DBC][r2dbc-spi].

## About

Provide listener for R2DBC query executions and method invocations.

### Listener API

- before/after query execution
- (operations on `Connection`, `Batch` and `Statement`) 

### Query logging

When query is executed by `Batch#execute()` or `Statement#execute()`, log query execution
information.

(wrapped for display purpose)
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

### Method tracing

When any methods on `Connection`, `Batch`, or `Statement` are invoked,
_"beforeMethod()/afterMethod()"_ on listener is called.


## Sample

This example shows how to print out invoked methods with some additional info.
Essentially, it displays interaction with R2DBC SPI for query execution.

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


## Install

- local maven install
  ```shell
  ./mvnw install
  ```

- [jitpack][jitpack]


## Setup

Wrap original `ConnectionFactory` by `ProxyConnectionFactory` and pass it to R2DBC client.

```java
// original connection factory
ConnectionFactory connectionFactory = new PostgresqlConnectionFactory(this.configuration);


// converters
QueryExecutionInfoFormatter queryExecutionFormatter = QueryExecutionInfoFormatter.showAll();
MethodExecutionInfoFormatter methodExecutionFormatter = MethodExecutionInfoFormatter.withDefault();

// create proxied connection factory
ConnectionFactory proxyConnectionFactory =
  ProxyConnectionFactory.create(connectionFactory)  // wrap original ConnectionFactory
    .onAfterMethod(execInfo -> {
      ...   // callback after method execution
    })  
    .onAfterQuery(execInfo -> {
      ...  //  callback after query execution
    });

// initialize client with the wrappd connection factory
this.r2dbc = new R2dbc(proxyConnectionFactory);
```

----

[datasource-proxy]: https://github.com/ttddyy/datasource-proxy
[jitpack]: https://jitpack.io/#ttddyy/datasource-proxy-r2dbc/
[r2dbc-spi]: https://github.com/r2dbc/r2dbc-spi 

