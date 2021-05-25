package com.fileservice.utility;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;

@Getter
@Setter
public class Message {

    private HashMap<String, Object> response;

    public Message() {
        response = new HashMap<>();
        response.put("date", LocalDateTime.now());
    }

    public Message withElement(String key, Object value) {
        response.put(key, value);
        return this;
    }

}
