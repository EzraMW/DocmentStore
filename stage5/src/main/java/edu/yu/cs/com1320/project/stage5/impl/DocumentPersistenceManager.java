package edu.yu.cs.com1320.project.stage5.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
//import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;
import org.apache.pdfbox.io.IOUtils;
import sun.nio.ch.IOUtil;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * created by the document store and given to the BTree via a call to BTree.setPersistenceManager
 */
public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {
    HashMap<URI, JsonElement> map = new HashMap<>();
    Type doctype = new TypeToken<Document>() {}.getType();
    File baseDir;

    public DocumentPersistenceManager(File baseDir){
        if (baseDir == null){
            this.baseDir = new File(System.getProperty("user.dir"));
        } else{
            this.baseDir = baseDir;
        }
    }

    @Override
    public void serialize(URI uri, Document doc) throws IOException {
        Gson gson = new GsonBuilder().registerTypeAdapter(Document.class, (JsonSerializer<Document>) (document, type, jsonSerializationContext) -> {
            JsonObject object = new JsonObject();
            String key = document.getKey().toString();
            String doctxt = document.getDocumentAsTxt();
            int hash = document.hashCode();
            Map<String,Integer> map = document.getWordMap();
            object.addProperty("key", key);
            object.addProperty("text", doctxt);
            object.addProperty("hashcode", hash);
            Gson g = new Gson();
            String mapstring = g.toJson(map);
            object.addProperty("map", mapstring);
            return object;
        }).create();
        String key = doc.getKey().toString();
        String begginingletter = uri.getAuthority();
        int beggining = key.indexOf(begginingletter);
        String secondname = key.substring(beggining);
        if (secondname.contains("/")){
            secondname = secondname.replace("/", File.separator);
        }
        String name = this.baseDir.toString().concat(File.separator + secondname);
        String filename = name.substring(name.lastIndexOf(File.separatorChar) + 1);
        String pathname = name.replace(filename, "");
        name = name.concat(".json");
        File previousfile = new File(pathname);
        String fileName = filename.concat(".json");
        boolean structured = previousfile.mkdirs();
        File file = new File(previousfile, fileName);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.flush();
        JsonWriter jsonWriter = gson.newJsonWriter(fileWriter);
        gson.toJson(doc, doctype, jsonWriter);
        jsonWriter.flush();
        fileWriter.close();
        jsonWriter.close();
        file.deleteOnExit();
    }

    @Override
    public Document deserialize(URI uri) throws IOException {
        Gson gson = new GsonBuilder().registerTypeAdapter(Document.class, (JsonDeserializer<Document>) (jsonElement, type, jsonDeserializationContext) -> {
            String name = jsonElement.getAsJsonObject().get("key").getAsString();
            URI uri1 = null;
            try {
                uri1 = new URI(name);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            String txt = jsonElement.getAsJsonObject().get("text").getAsString();
            int hash = jsonElement.getAsJsonObject().get("hashcode").getAsInt();
            String stringmap = jsonElement.getAsJsonObject().get("map").getAsString();
            Gson g = new Gson();
            HashMap map = g.fromJson(stringmap, HashMap.class);
            Document doc = new DocumentImpl(uri1, txt, hash, (HashMap<String, Integer>) map);
            return doc;
            }).create();
        // get file name and directory
        String begginingletter = uri.getAuthority();
        String key = uri.toString();
        int beggining = key.indexOf(begginingletter);
        String secondname = key.substring(beggining);
        if (secondname.contains("/")){
            secondname = secondname.replace("/", File.separator);
        }
        String name = this.baseDir.toString().concat(File.separator + secondname);
        String fullname = name.concat(".json");
        File previousfile = new File(fullname);
        if (!previousfile.exists()){
            return null;
        }
        InputStream is = new FileInputStream(fullname);
        InputStreamReader isr = new InputStreamReader(is);
        JsonStreamParser streamParser = new JsonStreamParser(isr);
        JsonElement element = streamParser.next();
        isr.close();
        is.close();
        // then delete all the directories
        // then make the document
        Document document = gson.fromJson(element, doctype);
        document.setLastUseTime(System.nanoTime());
        previousfile.delete();
        return document;
    }
}
