# datasource-proxy-r2dbc

[datasource-proxy][datasource-proxy] for [R2DBC][r2dbc-spi].

## About

Provide listener for R2DBC query executions and method invocations(TBD).

### Listener API

- before/after query execution
- (operations on `Connection`, `Batch` and `Statement`) 

### Query log example

When query is executed by `Batch#execute()` or `Statement#execute()`, log query execution
information.

(wrapped for display purpose)
```sql
# Statement with no bindings
# 
Thread:reactor-tcp-nio-1(30) Success:True Time:34 Type:Statement BatchSize:0 BindingsSize:0 
Query:["SELECT value FROM test"], Bindings:[]

# Batch query
#
Thread:reactor-tcp-nio-3(32) Success:True Time:4 Type:Batch BatchSize:2 BindingsSize:0
Query:["INSERT INTO test VALUES(200)","SELECT value FROM test"], Bindings:[]

# Statement with multiple bindings
#
Thread:reactor-tcp-nio-1(30) Success:True Time:21 Type:Statement BatchSize:0 BindingsSize:2
Query:["INSERT INTO test VALUES ($1)"], Bindings:[(100),(200)]
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

  Logger logger = Loggers.getLogger(getClass());

  // create listener   TODO: better API
  ProxyDataSourceListener listener = new ProxyDataSourceListener() {
      @Override
      public void afterQuery(ExecutionInfo execInfo) {
          // construct query log string
          String queryLog = new ExecutionInfoToString().apply(execInfo);

          System.out.println(queryLog);
          // logger.info(queryLog);  // or write it to logger
      }
  };

  // register listner
  ProxyConfig proxyConfig = new ProxyConfig();
  proxyConfig.addListener(listener);

  // wrapped connection factory
  ConnectionFactory proxyConnectionFactory =
            new ProxyConnectionFactory(connectionFactory, proxyConfig);

  // initialize client with the wrappd connection factory
  this.r2dbc = new R2dbc(proxyConnectionFactory);
```

----

[datasource-proxy]: https://github.com/ttddyy/datasource-proxy
[jitpack]: https://jitpack.io/#ttddyy/datasource-proxy-r2dbc/
[r2dbc-spi]: https://github.com/r2dbc/r2dbc-spi 

