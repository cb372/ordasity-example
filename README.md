Usage:

    git clone git@github.com:cb372/ordasity.git

    cd ordasity

    git checkout setState-fix

    mvn install

    cd ../ordasity-example

    sbt run

1. Start a ZK server on localhost:2181.

2. Start a few instances of the example, as shown above.

3. Start a ZK client and create work units under /work-units/. e.g. "create /work-units/#rpscala {}". 
    * zNode name = the keyword to search for on Twitter.

4. Start and stop a few instances of the example. Wait a few minutes and watch the nodes magically rebalance their work.

