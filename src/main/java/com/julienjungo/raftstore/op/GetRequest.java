package com.julienjungo.raftstore.op;

public record GetRequest(String op, String key) implements OpRequest {

  public static GetRequest from(String key) {
    return new GetRequest("get", key);
  }
}
