package com.julienjungo.raftstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;;

@SpringBootApplication
public class RaftStoreNode {

  public static void main(String[] args) {
    SpringApplication.run(RaftStoreNode.class, args);
  }
}
