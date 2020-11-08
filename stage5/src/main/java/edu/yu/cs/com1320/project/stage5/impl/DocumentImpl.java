package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.Document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class DocumentImpl implements Document {
    private int txtHash;
    private URI uri;
    private String txt;
    private byte[] pdfBytes;
    private int count;
    String[] words;
    HashMap<String,Integer> map = new HashMap<>();
    long time;
    int size;


    public DocumentImpl(URI uri, String txt, int txtHash){
        if (txt == null){
            throw new IllegalArgumentException();
        }
        this.uri = uri;
        this.txt = txt;
        this.txtHash = txtHash;
        this.size = txt.getBytes().length;
        this.getDocumentAsPdf();
        String messywords = txt.replaceAll("\\p{Punct}", "");
        messywords = messywords.toLowerCase();
        messywords = messywords.replace("– ", "");
        messywords = messywords.replace("(", "");
        messywords = messywords.replace(")", "");
        messywords = messywords.replace("”", "");
        messywords = messywords.replace("“", "");
        messywords = messywords.replace("]", "");
        messywords = messywords.replace("[", "");
        String[] wordsarray = messywords.split(" ");
        this.words = wordsarray;
        for (String w: wordsarray){
            w = w.trim();
            if (!map.containsKey(w)){
                this.map.put(w,1);
            }
            else{
                int i = map.get(w);
                this.map.put(w, ++i);
            }
        }
    }

    public DocumentImpl(URI uri, String txt, int txtHash, HashMap<String, Integer> wordmap) {
        if (txt == null) {
            throw new IllegalArgumentException();
        }
        this.uri = uri;
        this.txt = txt;
        this.txtHash = txtHash;
        this.size = txt.getBytes().length;
        this.getDocumentAsPdf();
        this.map = (HashMap<String, Integer>) wordmap;
    }

    protected String[] words(){
        return this.words;
    }

    int getSize(){
        return this.size;
    }

    public DocumentImpl(URI uri, String txt, int txtHash, byte[] pdfBytes){
        this.uri = uri;
        this.txt = txt;
        this.txtHash = txtHash;
        this.pdfBytes = pdfBytes;
        this.size = pdfBytes.length;
        String messywords = txt.replaceAll("\\p{Punct}", "");
//        String messywords = txt.replaceAll("^a-zA-Z0-9 ]", "");
        messywords = messywords.toLowerCase();
        messywords = messywords.replace("– ", "");
        messywords = messywords.replace("(", "");
        messywords = messywords.replace(")", "");
        messywords = messywords.replace("”", "");
        messywords = messywords.replace("“", "");
        String[] words = messywords.split(" ");
        this.words = words;
        for (String w: words){
            w = w.trim();
            if (!map.containsKey(w)){
                this.map.put(w,1);
            }
            else{
                int i = map.get(w);
                this.map.put(w, ++i);
            }
        }
    }


    @Override
    public int wordCount(String word) {
        word = word.toLowerCase();
        if (this.map.containsKey(word)){
            return this.map.get(word);
        }
        return 0;
    }

    /**
     * return the last time this document was used, via put/get or via a search result
     * (for stage 4 of project)
     */
    @Override
    public long getLastUseTime() {
        return this.time;
    }

    @Override
    public void setLastUseTime(long timeInNanoseconds) {
        this.time = timeInNanoseconds;
    }

    /**
     * @return a copy of the word to count map so it can be serialized
     */
    @Override
    public Map<String, Integer> getWordMap() {
        return this.map;
    }

    /**
     * This must set the word to count map during deserialization
     *
     * @param wordMap
     */
    @Override
    public void setWordMap(Map<String, Integer> wordMap) {
        this.map = (HashMap<String, Integer>) wordMap;
    }

    @Override
    public byte[] getDocumentAsPdf() {
        if (this.pdfBytes != null){
            return this.pdfBytes;
        } else {
            PDDocument pdfdoc = new PDDocument();
            PDPage page = new PDPage();
            pdfdoc.addPage(page);
            PDFont font = PDType1Font.TIMES_ROMAN;
            String txt = this.txt;
            txt = txt.trim();
            ByteArrayOutputStream byteout = new ByteArrayOutputStream();
            try {
                PDPageContentStream contents = new PDPageContentStream(pdfdoc, page);
                contents.beginText();
                contents.setFont(font, 12);
                contents.newLineAtOffset(100, 700);
                contents.showText(txt);
                contents.endText();
                contents.close();
                pdfdoc.save("golblygookbyte" + count);
                count++;
                pdfdoc.save(byteout);
                pdfdoc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return byteout.toByteArray();
        }
    }

    @Override
    public String getDocumentAsTxt() {
        if (this.txt == null){
            return null;
        }
        return this.txt.trim();
    }

    @Override
    public int getDocumentTextHashCode() {
        return this.txtHash;
    }

    @Override
    public URI getKey() {
        return this.uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentImpl document = (DocumentImpl) o;
        return txtHash == document.txtHash &&
                uri.equals(document.uri) &&
                txt.equals(document.txt) &&
                Arrays.equals(pdfBytes, document.pdfBytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(txtHash, uri, txt);
        result = 31 * result + Arrays.hashCode(pdfBytes);
        return result;
    }


    @Override
    public int compareTo(Document o) {
        if (o == null){
            throw new NullPointerException();
//            return 1;
        }
        if (!o.getClass().equals(this.getClass())){
            throw new ClassCastException();
        }

        if (this.getLastUseTime() > o.getLastUseTime()){
            return 1;
        } else if (this.getLastUseTime() < o.getLastUseTime()){
            return -1;
        } else if (this.getLastUseTime() == o.getLastUseTime()){
            return 0;
        }
        return Long.compare(this.getLastUseTime(), o.getLastUseTime());
    }
}