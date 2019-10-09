# lucene4lookup

Test Speed of Lucene 4 vs Lucene 8 lookups. 

Lucene 8

1000000 docs
***********
Indexing start string
Indexing complete string
search string complete in 9.713112046 secs
***********
Indexing start numeric
Indexing complete numeric
search numeric complete in 0.441230859 secs

Lucene 4

1000000 docs
***********
Indexing start string
Indexing complete string
search string complete in 4.7344909 secs
***********
Indexing start numeric
Indexing complete numeric

org.opentest4j.AssertionFailedError:
(Numerics dont work with Terms Filter)

Take away

    Lucene 8 numeric lookups are much faster!
    Lucene 8 string lookup is twice as slow as Lucene 4.
