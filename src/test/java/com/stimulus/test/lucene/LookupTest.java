package com.stimulus.test.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import com.google.common.primitives.Longs;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.util.Version;
import org.junit.jupiter.api.*;

import org.junit.jupiter.api.Assertions;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;


public class LookupTest {

    public static final int ROUNDS = 1000000;
    private static final String CHARLIST = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
    private Path indexDirectoryPath;
    private static Random r = new Random();
    protected static final HashFunction hf = Hashing.murmur3_128();

    public LookupTest()  {
    }

    public String genStr(Random random, int length) {
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = CHARLIST.charAt(random.nextInt(CHARLIST.length()));
        }
        return new String(text);
    }
    @Test
    public void testLookup() throws Exception {
        try {
            init();
            System.out.println("***********");
            index(false);
            search(false);
            System.out.println("***********");
            index(true);
            search(true);
        } finally {
            cleanup();
        }
    }

    private void init() throws IOException {
        this.indexDirectoryPath = Files.createTempDirectory("lookup");
        System.out.println(ROUNDS + " docs");
    }

    private void cleanup() throws IOException {
        if (Objects.nonNull(indexDirectoryPath))
            MoreFiles.deleteRecursively(indexDirectoryPath, RecursiveDeleteOption.ALLOW_INSECURE);
    }

    private void index(boolean numeric) throws IOException {

        IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, new StandardAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try ( Directory indexDirectory = FSDirectory.open(indexDirectoryPath.toFile());
              IndexWriter writer = new IndexWriter(indexDirectory, config)) {
            System.out.println("Indexing start " + sig (numeric));
            for (int i = 0; i < ROUNDS; i++) {
                Document doc = new Document();

                if (numeric) {
                    doc.add( new LongField("uid", idNum(i), Field.Store.YES));
                    doc.add(new StringField("crap", genStr(r, 256), Field.Store.YES));
                } else {
                    doc.add( new StringField("uid", id(i), Field.Store.YES));
                    doc.add(new StringField("crap", genStr(r, 256), Field.Store.YES));
                }
                //doc.add(new SortedDocValuesField("uid", new BytesRef(id(i))));

                writer.addDocument(doc);
            }
            System.out.println("Indexing complete " + sig (numeric));
        }
    }

    private String id(int i) {
        HashCode hashCode = hf.newHasher().putInt(i).hash();
        return Long.toHexString(hashCode.padToLong());
    }


    private long idNum(int i) {
        HashCode hashCode = hf.newHasher().putInt(i).hash();
        return hashCode.padToLong();
    }

    private String[] ids() {
        String[] ids = new String[ROUNDS];
        for (int i = 0 ; i < ROUNDS; i ++) {
            ids[i] = id(i);
        }
        return ids;
    }


    private long[] idNums() {
       long[] ids = new long[ROUNDS];
        for (int i = 0 ; i < ROUNDS; i ++) {
            ids[i] = idNum(i);
        }
        return ids;
    }

    private String sig(boolean numeric) {
        return (numeric ? "numeric" : "string");
    }


    private Term toLong(long id) {
        return new Term("uid", new BytesRef(Longs.toByteArray(id)));
    }

    private void search(boolean numeric) throws IOException {
        try (Directory searchDirectory = FSDirectory.open(indexDirectoryPath.toFile());
             IndexReader reader = DirectoryReader.open(searchDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocsCollector<?> tfc = TopScoreDocCollector.create(ROUNDS, false);
            long start;

            List<Term> terms = new ArrayList<>();
            int actualLen;
            if (numeric) {
                long[] ids = idNums();
                for (int i = 0 ; i < ids.length; i++)
                    terms.add(toLong(ids[i]));
                actualLen = ids.length;

            } else {
                String[] ids = ids();
                for (int i = 0 ; i < ids.length; i++)
                    terms.add(new Term("uid",ids[i]));
                actualLen = ids.length;
            }
            start =  System.nanoTime();
            searcher.search(new MatchAllDocsQuery(), new IDFilter(terms), tfc);
            Assertions.assertEquals(actualLen, tfc.getTotalHits());
            long end = System.nanoTime();
            double elapsedTimeInSecond = (double) (end-start) / 1_000_000_000;
            System.out.println("search "+ sig (numeric) + " complete in "+elapsedTimeInSecond+" secs");
        }

    }

}