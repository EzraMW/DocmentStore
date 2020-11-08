package edu.yu.cs.com1320.project;


import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;


public interface BTree<Key extends Comparable<Key>, Value>
{
    Value get(Key k);
    Value put(Key k, Value v);
    void moveToDisk(Key k) throws Exception;
//    void setSerializer(JsonSerializer<Key> js);
//    void setDeserializer (JsonDeserializer<Key> jd);
    void setPersistenceManager(PersistenceManager<Key,Value> pm);
}