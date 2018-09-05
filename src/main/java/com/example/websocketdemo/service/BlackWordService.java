package com.example.websocketdemo.service;

import com.example.websocketdemo.model.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
public class BlackWordService {

    @Value("${spring.data.mongodb.uri}")
    private String mongodbUri;

    @Autowired
    private ObjectMapper objectMapper;

    public boolean hasBlackWord(String blockWord) {
        try (MongoClient mongoClient = new MongoClient(new MongoClientURI(mongodbUri))) {
            MongoDatabase db = mongoClient.getDatabase("gsshop");
            if (db.getCollection("black_word").countDocuments(new Document("word", blockWord)) > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean addBlackWord(ChatMessage msg, String blockWord) throws JsonProcessingException {
        // 금지어가 들어간 메시지는 모두 저장 한다.
        try (MongoClient mongoClient = new MongoClient(new MongoClientURI(mongodbUri))) {
            MongoDatabase db = mongoClient.getDatabase("gsshop");
            db.getCollection("message").insertOne(Document.parse(objectMapper.writeValueAsString(msg))
                    .append("date", new Date()));
            // 새로운 금지어의 경우 저장
            if (!hasBlackWord(blockWord)) {
                db.getCollection("black_word").insertOne(new Document("word", blockWord).append("date", new Date()));
                return true;
            }
        }
        return false;
    }

    public HashSet<String> getBlackWordList() {
        try (MongoClient mongoClient = new MongoClient(new MongoClientURI(mongodbUri))) {
            MongoDatabase db = mongoClient.getDatabase("gsshop");
            return new HashSet<String>(db.getCollection("black_word").find().into(new ArrayList<Document>())
                    .stream().map(d -> d.getString("word")).collect(Collectors.toList()));
        }
    }

}
