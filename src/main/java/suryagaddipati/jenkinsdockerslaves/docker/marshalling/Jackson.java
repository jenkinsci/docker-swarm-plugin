
package suryagaddipati.jenkinsdockerslaves.docker.marshalling;

import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.MediaTypes;
import akka.http.javadsl.model.RequestEntity;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.IOException;
import java.util.List;

public class Jackson {

  private static final ObjectMapper defaultObjectMapper = new ObjectMapper();
  static{
    defaultObjectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
    defaultObjectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    defaultObjectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    defaultObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
  }

  public static <T> Marshaller<T, RequestEntity> marshaller(ObjectMapper mapper) {
    return Marshaller.wrapEntity(
            u -> toJSON(mapper, u),
            Marshaller.stringToEntity(),
            MediaTypes.APPLICATION_JSON
    );
  }

  public static <T> Unmarshaller<HttpEntity, T> unmarshaller(Class<T> expectedType) {
    return unmarshaller(defaultObjectMapper, expectedType);
  }

  public static <T> Unmarshaller<HttpEntity, T> unmarshaller(ObjectMapper mapper, Class<T> expectedType) {
    return Unmarshaller.forMediaType(MediaTypes.APPLICATION_JSON, Unmarshaller.entityToString())
            .thenApply(s -> fromJSON(mapper, s, expectedType));
  }

  private static String toJSON(ObjectMapper mapper, Object object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Cannot marshal to JSON: " + object, e);
    }
  }

  private static <T> T fromJSON(ObjectMapper mapper, String json, Class<T> expectedType) {
    try {
      return mapper.readerFor(expectedType).readValue(json);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot unmarshal JSON as " + expectedType.getSimpleName(), e);
    }
  }

  private static <T> T fromJSONArray(ObjectMapper mapper, String json, Class<T> expectedType) {
    try {
      CollectionType arrayType = mapper.getTypeFactory()
              .constructCollectionType(List.class, expectedType);
      return mapper.readerFor(arrayType).readValue(json);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot unmarshal JSON as " + expectedType.getSimpleName(), e);
    }
  }


  public static Unmarshaller<HttpEntity,?> unmarshaller(Class<?> responseClass, ResponseType responseType) {
    if(responseType == ResponseType.CLASS){
      return unmarshaller(responseClass);
    }else {
      return Unmarshaller.forMediaType(MediaTypes.APPLICATION_JSON, Unmarshaller.entityToString())
              .thenApply(s -> fromJSONArray(defaultObjectMapper, s, responseClass));
    }
  }
  public static ObjectMapper getDefaultObjectMapper() {
    return defaultObjectMapper;
  }
}
