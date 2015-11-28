package edu.illinois.cs.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Indexer {

    /**
     * Creates the initial index files on disk
     *
     * @param indexPath
     * @return
     * @throws IOException
     */
    private static IndexWriter setupIndex(String indexPath) throws IOException {
        Analyzer analyzer = new SpecialAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46,
                analyzer);
        config.setOpenMode(OpenMode.CREATE);
        config.setRAMBufferSizeMB(2048.0);

        FSDirectory dir;
        IndexWriter writer = null;
        dir = FSDirectory.open(new File(indexPath));
        writer = new IndexWriter(dir, config);

        return writer;
    }

    /**
     * @param indexPath
     *            Where to create the index
     * @param prefix
     *            The prefix of all the paths in the fileList
     * @param fileList
     *            Each line is a path to a document
     * @throws IOException
     */
    public static void index(String indexPath, String prefix, String fileList)
            throws IOException {

        System.out.println("Creating Lucene index...");

        FieldType _contentFieldType = new FieldType();
        _contentFieldType.setIndexed(true);
        _contentFieldType.setStored(true);

        IndexWriter writer = setupIndex(indexPath);
        BufferedReader br = new BufferedReader(
                new FileReader(prefix + fileList));
        String line = null;
        int indexed = 0;
        while ((line = br.readLine()) != null) {

            Document doc = new Document();
            doc.add(new Field("content", line, _contentFieldType));
            writer.addDocument(doc);

            ++indexed;
            if (indexed % 100 == 0)
                System.out.println(" -> indexed " + indexed + " docs...");
        }
        System.out.println(" -> indexed " + indexed + " total docs.");

        br.close();
        writer.close();
    }
}
