package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.Undoable;
//import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.*;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.function.Function;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class DocumentStoreImpl implements DocumentStore {
    private InputStream input;
    private URI uri;
    private DocumentFormat format;
    private BTreeImpl<URI, Document> btree;
    private DocumentImpl doc;
    private StackImpl<Undoable> stack;
    private TrieImpl<URI> trie;
    private int doclimit;
    private int sizelimit;
    private int size;
    private int docs;
    ArrayList<URI> storeuris;
    HashMap<URI, Long> map = new HashMap<>();
    CompareNode cn = new CompareNode(this.map);
    private MinHeapImpl<URI> heap;
    private PDFTextStripper stripper; {
        try {
            stripper = new PDFTextStripper();
        } catch (IOException e) {
            e.printStackTrace();} }

    public DocumentStoreImpl(){
        stack = new StackImpl<>();
        File baseDir = new File(System.getProperty("user.dir"));
        this.btree = new BTreeImpl<URI, Document>();
        trie = new TrieImpl<>();
        heap = new MinHeapImpl<>();
        this.size = 0;
        this.docs = 0;
        this.sizelimit = Integer.MAX_VALUE;
        this.doclimit = Integer.MAX_VALUE;
        storeuris = new ArrayList<>();
        DocumentPersistenceManager documentPersistenceManager = new DocumentPersistenceManager(baseDir);
        btree.setPersistenceManager(documentPersistenceManager);
        URI sentinal = URI.create("0");
        btree.put(sentinal, null);
    }

    public DocumentStoreImpl(File baseDir){
        this.btree = new BTreeImpl<URI, Document>();
        stack = new StackImpl<>();
        trie = new TrieImpl<>();
        heap = new MinHeapImpl<>();
        this.size = 0;
        this.docs = 0;
        this.sizelimit = Integer.MAX_VALUE;
        this.doclimit = Integer.MAX_VALUE;
        storeuris = new ArrayList<>();
        DocumentPersistenceManager documentPersistenceManager = new DocumentPersistenceManager(baseDir);
        btree.setPersistenceManager(documentPersistenceManager);
        URI sentinal = URI.create("0");
        btree.put(sentinal, null);
    }

    private TrieImpl<URI> getTrie(){
        return this.trie;
    }
    protected ArrayList<URI> getStoreUris() {return this.storeuris;}

    private void setMemory() {
        this.docs = this.storeuris.size();
        while (this.docs > this.doclimit || this.size > this.sizelimit) {
            cn.updateMap(this.map);
            URI uri = heap.removeMin();
            storeuris.remove(uri);
            DocumentImpl doc = (DocumentImpl) btree.get(uri);
            try {
                btree.moveToDisk(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.size = size - doc.getSize();
            this.docs--;
        }
    }

    protected void deleteDirectory(URI uri){
        // this method should delete directories at the end of a test and the json file for that uri.
        // Specifically used for docuemnts moved to disk that shouldnt have been deserialized, in which case
        // they directory would be removed anyway
        // Thus, by calling btree.get for this uri it checks if it has been searliazed and not deserialzied
        // and if it has it desearlizes it and deletes its directories and file by putting it back into the
        // btree as a document, but that last part doesn't matter cuz im done with the docstore
        btree.get(uri);
    }


    protected int getSize(){
        return this.size;
    }
    protected int getDocs(){
        return this.docs;
    }
    protected BTreeImpl<URI, Document> getBTree() {return this.btree;}

    protected MinHeapImpl<URI> getHeap(){
        return this.heap;
    }

    @Override
    public int putDocument(InputStream input, URI uri, DocumentFormat format) {
        if (uri == null || format == null){
            throw new IllegalArgumentException();
        } if (input == null){
            // Delete document by "put"ing a null value into the hashtable
            int hash;
            if (this.getDocumentAsTxt(uri) == null){
                hash = 0;
                Function<URI, Boolean> f = new Function<URI, Boolean>() {
                    @Override
                    public Boolean apply(URI uri) {
                        return true;
                    } };
                GenericCommand<URI> c = new GenericCommand<>(uri, f);
                this.stack.push(c);
            } else {
                hash = this.getDocumentAsTxt(uri).hashCode();
                this.deleteDocument(uri);
            }
            //return hashcode of this document being deleted
            return hash;
        } this.input = input; this.uri = uri; this.format = format;
        int oldhash = 0;
        if (getDocumentAsTxt(uri) != null){
            oldhash = getDocumentAsTxt(uri).hashCode();
        }
        //hash the uri to get the key
        //find where on the hashtable the document is supposed to go by
        // hashing the uri to get they key and there the hash function will get the array index and
        // add it to the end of the list within that index
        Document doc = makeNewDoc(input, uri, format);
        if (doc == null){
            return 0;
        } Function<URI, Boolean> f = new Function<URI, Boolean>() {
            @Override
            public Boolean apply(URI uri) {
                DocumentImpl doc = (DocumentImpl) getDocument(uri);
//                hasht.put(uri, null);
                btree.put(uri, null);
                doc.setLastUseTime(Long.MIN_VALUE);
                map.put(uri, Long.MIN_VALUE);
                heap.reHeapify(uri);
                heap.removeMin();
                size = size - doc.getSize();
                docs--;
                storeuris.remove(uri);
//                storedocs.remove(doc);
//                setMemory();
//                deleteDocument(uri);
                return stack.size() != 0;
            }
        }; GenericCommand<URI> gc = new GenericCommand<>(uri,f);
        stack.push( gc);
//        DocumentImpl existed = this.hasht.put(uri, doc);
        Document existed = this.btree.put(uri, doc);
        if (existed == null){
            return 0;
        } else{
            return oldhash;
        }
    }

    protected Document makeNewDoc(InputStream input, URI uri, DocumentStore.DocumentFormat format) {
        byte[] txtbyte = this.textToByteArray(input);
        InputStream i = new ByteArrayInputStream(txtbyte);
        String stringtxt = new String(txtbyte);
        stringtxt = stringtxt.trim();
        int txthash = stringtxt.hashCode();
        DocumentImpl doc = null;
        if (format == DocumentStore.DocumentFormat.TXT) {
            doc = new DocumentImpl(uri, stringtxt, txthash);
        } else if (format == DocumentStore.DocumentFormat.PDF) {
            String pdfstring = getTextOfPdf(i);
            doc = new DocumentImpl(uri, pdfstring, txthash, txtbyte);
        }
        if (doc != null){
            if (doc.size > this.sizelimit){
                // this is a little strange but should work. I am putting this doc into the btree now so
                // that i can move it to the disk and then returning as null for doc so that when it adds
                // it in to the btree again in the end of the put method it will just re-add the same null
                // to the same URI
                btree.put(uri,doc);
                try {
                    btree.moveToDisk(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
            doc.setLastUseTime(System.nanoTime());
            map.put(uri, System.nanoTime());
            if (!storeuris.contains(uri)){
                addDocToHeap(doc);
                addDocToTrie(doc);
            }
        }
        return doc;
    }

    protected void addDocToHeap(DocumentImpl doc){
        // set memory so that docs is how many documents are in the store and therefore the heap
        // and the size reflects how many bytes have been taken up by all the documents in the store
//        setMemory();
        // the doc has rly already been placed in the store so now we can evaluate it with respect to the heap.
        // So long as the amount of docs in the store or its size are over the limit then we must remove
        // the least recently accessed doc (with the lowest time value, i.e. the min) from the heap and delete it.
        // However, if the amount of docs in the store is less than the limit (including this one) and the current amount of
        // bytes of all the docs is less than the limit, then everything is good and put it into the heap and it
        // will find the right place to be
        setMemory();
        URI a = doc.getKey();
        Long time = doc.getLastUseTime();
        this.map.put(a,time);
        cn.updateMap(this.map);
        this.heap.insert(a);
        this.storeuris.add(a);
        this.docs++;
        this.size += doc.getSize();
        // not sure if this is neccesary
        heap.reHeapify(a);
        setMemory();
    }

    protected void addDocToTrie(Document doc){
        DocumentImpl document = (DocumentImpl) doc;
        if (doc != null) {
            String[] wordarray = document.words();
            if (wordarray != null) {
                for (String s : wordarray) {
                    trie.put(s, doc.getKey());
                }
            }
        }
    }

    private String getTextOfPdf(InputStream input) {
        String pdftxt = null;
        try {
            PDDocument pdocument = PDDocument.load(input);
            pdftxt = stripper.getText(pdocument);
            pdocument.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pdftxt;
    }

    private byte[] textToByteArray(InputStream input) {
        byte[] barray;
        ByteArrayOutputStream byteout = new ByteArrayOutputStream();
        try {
            int num = input.available();
            barray = new byte[input.available()*3];
            while (this.input.read(barray) != -1){
                byteout.write(barray, 0,num);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteout.toByteArray();
    }

    @Override
    public byte[] getDocumentAsPdf(URI uri) {
        if (btree.get(uri) == null){
            return null;
        }
        Document doc = btree.get(uri);
        if (!storeuris.contains(uri)){
            DocumentImpl document = (DocumentImpl) doc;
            addDocToHeap(document);
        }
        Long time = System.nanoTime();
        doc.setLastUseTime(time);
        map.put(uri, time);
        setMemory();
        heap.reHeapify( uri);
        return doc.getDocumentAsPdf();
    }

    @Override
    public String getDocumentAsTxt(URI uri) {
        if (btree.get(uri) == null){
            return null;
        }
        Document doc = btree.get(uri);
        if (!storeuris.contains(uri)){
            DocumentImpl document = (DocumentImpl) doc;
            addDocToHeap(document);
        }
        Long time = System.nanoTime();
        doc.setLastUseTime(time);
        map.put(uri, time);
        setMemory();
        heap.reHeapify( uri);
        return doc.getDocumentAsTxt();
    }

    @Override
    public boolean deleteDocument(URI uri) {
//        DocumentImpl doc = hasht.get(uri);
        Document doc = btree.get(uri);
        // this is just to make sure that calling delete on doc which has been deleted or nvr existed returns false
        if (doc == null || map.get(uri) == null){
            return false;
        }
        // To create the option to later undo this delete I use the lamda expression of the function class to solidify
        // what it would take to re-put this document in the hashtable. So I use the uri to get the doc and call the put
        // method on hashtable on this document with this uri to put it back in
        Function<URI, Boolean> f = new Function<URI, Boolean>() {
            @Override
            public Boolean apply(URI uri) {
                btree.put(uri, doc);
                addDocToTrie(doc);
                doc.setLastUseTime(System.nanoTime());
                map.put(uri, System.nanoTime());
                addDocToHeap((DocumentImpl) doc);
                setMemory();
                return true;
            }
        };
        GenericCommand<URI> c = new GenericCommand<>(uri, f);
        stack.push(c);
        Document deleted = btree.get(uri);
        if (!storeuris.contains(uri)){
            DocumentImpl document = (DocumentImpl) doc;
            addDocToHeap(document);
        }
        btree.put(uri, null);
        doc.setLastUseTime(Long.MIN_VALUE);
        map.put(uri, Long.MIN_VALUE);
        cn.updateMap(this.map);
        heap.reHeapify(uri);
        URI uri1 = heap.removeMin();
        storeuris.remove(uri);
        this.docs--;
        if (deleted != null){
            DocumentImpl doc1 = (DocumentImpl) deleted;
            this.size -= doc1.getSize();
        }
        return deleted != null;
    }

    @Override
    public void undo() throws IllegalStateException {
        if (stack.peek() == null){
            throw new IllegalStateException();
        }
        Undoable command = this.stack.pop();
        if (command instanceof GenericCommand){
            GenericCommand<URI> c = (GenericCommand<URI>) command;
            DocumentImpl doc = (DocumentImpl) getDocument(c.getTarget());
            doc.setLastUseTime(System.nanoTime());
            map.put(c.getTarget(), System.nanoTime());
            heap.reHeapify(c.getTarget());
            command.undo();
        } else if (command instanceof CommandSet){
            CommandSet<URI> c = (CommandSet<URI>) command;
            long time = System.nanoTime();
            Iterator<GenericCommand<URI>> i = c.iterator();
            for (; i.hasNext(); ) {
                GenericCommand<URI> gc = i.next();
                DocumentImpl doc = (DocumentImpl) getDocument(gc.getTarget());
                if (doc != null){
                    doc.setLastUseTime(time);
                    map.put(gc.getTarget(), time);
                    heap.reHeapify(gc.getTarget());
                }
                gc.undo();
            }
        }
    }

    @Override
    public void undo(URI uri) throws IllegalStateException {
        if (stack.peek() == null) {
            throw new IllegalStateException();
        }
        StackImpl<Undoable> tempstack = new StackImpl<>();
        StackImpl<Undoable> reversestack = new StackImpl<>();
        if (stack.peek() != null) {
            if (stack.peek() instanceof GenericCommand) {
                while (((GenericCommand<URI>) stack.peek()).getTarget() != uri) {
                    GenericCommand<URI> c = (GenericCommand<URI>) this.stack.pop();
                    tempstack.push(c);
                }
                if (((GenericCommand<URI>) stack.peek()).getTarget() == (uri)) {
                    Undoable u = this.stack.pop();
                    u.undo();
                }
            } else if (stack.peek() instanceof CommandSet) {
                while (!((CommandSet) stack.peek()).containsTarget(uri)) {
                    CommandSet<URI> c = (CommandSet<URI>) this.stack.pop();
                    tempstack.push(c);
                } if (((CommandSet<URI>) stack.peek()).containsTarget(uri)) {
                    CommandSet<URI> u = (CommandSet<URI>) this.stack.peek();
                    u.undo(uri); } }
            while (tempstack.peek() != null) {
                Undoable c = tempstack.pop();
                reversestack.push(c);
            }
            while (reversestack.peek() != null) {
                Undoable c = reversestack.pop();
                this.stack.push(c);
            }
        }
    }

    @Override
    public List<String> search(String keyword) {
        // get a list of all the documents which have this exact keyword in them from trie. Then iterate through
        // and add each's txt to a list of strings and then return it
        SortByWordInstances<URI> comperator = new SortByWordInstances<>(keyword, this.btree);
        List<URI> urilist = null;
        if (comperator != null){
            urilist = trie.getAllSorted(keyword, (Comparator<URI>)comperator);
        }
        if (urilist == null || urilist.size() == 0){
            return new ArrayList<>();
        }
        ArrayList<String> stringlist = new ArrayList<>();
        long time = System.nanoTime();
        for (URI uri: urilist){
            Document doc = btree.get(uri);
            if (!storeuris.contains(uri)){
                DocumentImpl document = (DocumentImpl) doc;
                addDocToHeap(document);
            }
            setMemory();
            stringlist.add(doc.getDocumentAsTxt());
            doc.setLastUseTime(time);
            map.put(uri, time);
            heap.reHeapify(uri);
        }
        return stringlist;
    }

    @Override
    public List<byte[]> searchPDFs(String keyword) {
        SortByWordInstances<URI> comperator = new SortByWordInstances<>(keyword, this.btree);
        List<URI> urilist = trie.getAllSorted(keyword, comperator);
        ArrayList<byte[]> pdflist = new ArrayList<>();
        long time = System.nanoTime();
        for(URI uri: urilist){
            Document doc = btree.get(uri);
            setMemory();
            if (doc != null){
                pdflist.add(doc.getDocumentAsPdf());
                doc.setLastUseTime(time);
            }
            heap.reHeapify(uri);
        }
        return pdflist;
    }

    @Override
    public List<String> searchByPrefix(String keywordPrefix) {
        SortByWordInstances<DocumentImpl> comperator = new SortByWordInstances<>(keywordPrefix, this.btree);
        List<URI> urilist = null;
        if (comperator != null){
            urilist = trie.getAllWithPrefixSorted(keywordPrefix, comperator);
        } ArrayList<String> stringlist = new ArrayList<>();
        if (urilist == null){
            return stringlist;
        }
        long time = System.nanoTime();
        for(URI uri: urilist){
            Document doc = btree.get(uri);
            setMemory();
            if (doc != null){
                stringlist.add(doc.getDocumentAsTxt());
                doc.setLastUseTime(time);
                map.put(uri, time);
                heap.reHeapify(uri);
            }
        }
        return stringlist;
    }

    @Override
    public List<byte[]> searchPDFsByPrefix(String keywordPrefix) {
        SortByWordInstances<DocumentImpl> comperator = new SortByWordInstances<>(keywordPrefix, this.btree);
        List<URI> urilist = trie.getAllWithPrefixSorted(keywordPrefix, comperator);
        ArrayList<byte[]> pdflist = new ArrayList<>();
        long time = System.nanoTime();
        for(URI uri: urilist){
            Document doc = btree.get(uri);
            setMemory();
            if (doc != null){
                pdflist.add(doc.getDocumentAsPdf());
                doc.setLastUseTime(time);
                map.put(uri, time);
            }
            heap.reHeapify(uri);
        }
        return pdflist;
    }

    @Override
    public Set<URI> deleteAll(String key) {
        Set<URI> dset= trie.deleteAll(key);
        if (dset == null) {
            return new HashSet<>();
        }
        CommandSet<URI> cset = new CommandSet<>();
        HashSet<URI> uriset = new HashSet<>();
        for (URI uri: dset){
            Document doc = btree.get(uri);
            setMemory();
            uriset.add(uri);
            Function<URI, Boolean> f = new Function<URI, Boolean>() {
                @Override
                public Boolean apply(URI uri) {
                    btree.put(uri, doc);
                    addDocToTrie(doc);
                    DocumentImpl document = (DocumentImpl) doc;
                    addDocToHeap(document);
                    doc.setLastUseTime(System.nanoTime());
                    map.put(uri, System.nanoTime());
                    heap.reHeapify(uri);
                    return true;
                }
            };
            this.deleteDocument(uri);
            GenericCommand<URI> c = new GenericCommand<>(uri, f);
            cset.addCommand(c);
        }
        stack.push(cset);
        return uriset;
    }

    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        Set<URI> dset= trie.deleteAllWithPrefix(keywordPrefix);
        HashSet<URI> uriset = new HashSet<>();
        CommandSet<URI> cset = new CommandSet<>();
        for (URI uri: dset){
            Document doc = btree.get(uri);
            setMemory();
            uriset.add(uri);
            Function<URI, Boolean> f = new Function<URI, Boolean>() {
                @Override
                public Boolean apply(URI uri) {
                    btree.put(uri, doc);
                    addDocToTrie(doc);
                    DocumentImpl document = (DocumentImpl) doc;
                    addDocToHeap(document);
                    doc.setLastUseTime(System.nanoTime());
                    map.put(uri, System.nanoTime());
                    heap.reHeapify(uri);
                    return true;
                }
            };
            this.deleteDocument(uri);
            GenericCommand<URI> c = new GenericCommand<>(uri, f);
            cset.addCommand(c);
        }
        stack.push( cset);
        return uriset;
    }

    /**
     * @return the Document object stored at that URI, or null if there is no such
    Document */
    protected Document getDocument(URI uri){
        if (!storeuris.contains(uri)){
            return null;
        } else {
            return btree.get(uri);
        }
    }

    /**
     * set maximum number of documents that may be stored
     *
     * @param limit
     */
    @Override
    public void setMaxDocumentCount(int limit) {
        this.doclimit = limit;
        setMemory();
        while (this.docs > this.doclimit){
            URI uri = heap.removeMin();
            storeuris.remove(uri);
            this.docs--;
            DocumentImpl doc = (DocumentImpl) btree.get(uri);
            this.size = size - doc.getSize();
            try {
                btree.moveToDisk(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
            setMemory();
        }
    }

    protected void deleteTotal(URI uri) {
        DocumentImpl doc = (DocumentImpl) btree.get(uri);
        if (doc == null){
            return;
        }
        btree.put(uri, null);
        doc.setLastUseTime(Long.MIN_VALUE);
        map.put(uri, Long.MIN_VALUE);
        heap.reHeapify(uri);
        heap.removeMin();
        storeuris.remove(uri);
        this.size = size - doc.getSize();
        this.docs--;
        setMemory();
    }
    /**
     * set maximum number of bytes of memory that may be used by all the documents in memory combined
     *
     * @param limit
     */
    @Override
    public void setMaxDocumentBytes(int limit) {
        this.sizelimit = limit;
        setMemory();
        while (this.size > this.sizelimit){
            URI uri = heap.removeMin();
            storeuris.remove(uri);
            this.docs--;
            DocumentImpl doc = (DocumentImpl) btree.get(uri);
            this.size = size - doc.getSize();
            try {
                btree.moveToDisk(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
            setMemory();
        }
    }
}