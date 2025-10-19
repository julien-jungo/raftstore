package com.julienjungo.raftstore.op;

public record SetRequest(String op, String key, String val) implements OpRequest {

  public static SetRequest from(String key, String val) {
    return new SetRequest("set", key, val);
  }
}
