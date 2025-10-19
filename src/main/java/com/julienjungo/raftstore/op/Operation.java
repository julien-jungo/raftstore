package com.julienjungo.raftstore.op;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public interface Operation {

  ObjectMapper MAPPER = new ObjectMapper();

  String toJson() throws IOException;
}
