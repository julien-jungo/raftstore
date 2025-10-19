package com.julienjungo.raftstore.op;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.julienjungo.raftstore.err.OpResponseMarshallingException;
import com.julienjungo.raftstore.err.OpResponseUnmarshallingException;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "op"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GetResponse.class, name = "get"),
        @JsonSubTypes.Type(value = SetResponse.class, name = "set"),
})
public sealed interface OpResponse extends Operation permits GetResponse, SetResponse {

  static OpResponse fromJson(String json) throws OpResponseUnmarshallingException {
    try {
      return MAPPER.readValue(json, OpResponse.class);
    } catch (JsonProcessingException e) {
      throw new OpResponseUnmarshallingException(e.getMessage());
    }
  }

  @Override
  default String toJson() throws OpResponseMarshallingException {
    try {
      return MAPPER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new OpResponseMarshallingException(e.getMessage());
    }
  }
}
