package com.example.websocketdemo.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
public class BlackWordService {
    private HashSet<String> blackWordList = new HashSet<String>();

    public boolean addBlackWord(String word){
        if(!blackWordList.contains(word)){
            blackWordList.add(word);
            return true;
        }
        return false;
    }

    public HashSet<String> getBlackWordList(){
        return (HashSet<String>)blackWordList.clone();
    }
}
