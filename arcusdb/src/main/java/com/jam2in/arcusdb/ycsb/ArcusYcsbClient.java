package com.jam2in.arcusdb.ycsb;

import com.jam2in.arcusdb.client.ArcusDbClient;
import com.jam2in.arcusdb.client.ArcusDbClientConfig;
import com.jam2in.arcusdb.client.Filters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import site.ycsb.ByteIterator;
import site.ycsb.DB;
import site.ycsb.DBException;
import site.ycsb.Status;
import site.ycsb.StringByteIterator;

/**
 * YCSB binding for ArcusDB.
 */
public class ArcusYcsbClient extends DB {

  public static final String HOST_PROPERTY = "arcusdb.host";
  public static final String PORT_PROPERTY = "arcusdb.port";
  public static final String TIMEOUT_MILLIS_PROPERTY = "arcusdb.timeoutMillis";

  public static final String DEFAULT_HOST = "127.0.0.1";
  public static final String DEFAULT_PORT = "17191";
  public static final String DEFAULT_TIMEOUT_MILLIS = "60000";

  private static final String ID_FIELD = "_id";
  private static final Logger LOGGER =
      Logger.getLogger(ArcusYcsbClient.class.getName());

  private ArcusDbClient client;
  private boolean closeClient;

  public ArcusYcsbClient() {
    this.closeClient = true;
  }

  public ArcusYcsbClient(ArcusDbClient client) {
    this.client = Objects.requireNonNull(client, "client");
    this.closeClient = false;
  }

  @Override
  public void init() throws DBException {
    if (client != null) {
      return;
    }

    try {
      Properties properties = getProperties();
      String host = properties.getProperty(HOST_PROPERTY, DEFAULT_HOST);
      int port = Integer.parseInt(
          properties.getProperty(PORT_PROPERTY, DEFAULT_PORT));
      long timeoutMillis = Long.parseLong(
          properties.getProperty(TIMEOUT_MILLIS_PROPERTY,
              DEFAULT_TIMEOUT_MILLIS));

      client = new ArcusDbClient(
          new ArcusDbClientConfig(host, port, timeoutMillis));
      closeClient = true;

      String name = "usertable";
      Document document = new Document("primary_key", "_id")
          .append("name", name)
          .append("required", List.of())
          .append("indexes", new Document());
      client.createCollection(name, document);
    } catch (RuntimeException e) {
      throw new DBException("Failed to initialize ArcusDB client.", e);
    }
  }

  @Override
  public void cleanup() throws DBException {
    if (client == null || !closeClient) {
      return;
    }

    try {
      client.close();
    } catch (RuntimeException e) {
      throw new DBException("Failed to close ArcusDB client.", e);
    } finally {
      client = null;
    }
  }

  @Override
  public Status read(String table, String key, Set<String> fields,
      Map<String, ByteIterator> result) {
    try {
      List<Document> documents = client.find(table, Filters.eq(ID_FIELD, key));
      if (documents.isEmpty()) {
        return Status.NOT_FOUND;
      }

      copyFields(documents.get(0), fields, result);
      return Status.OK;
    } catch (RuntimeException e) {
      LOGGER.log(Level.WARNING, "ArcusDB read failed for key: " + key, e);
      return Status.ERROR;
    }
  }

  @Override
  public Status scan(String table, String startkey, int recordcount,
      Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
    return Status.NOT_IMPLEMENTED;
  }

  @Override
  public Status update(String table, String key,
      Map<String, ByteIterator> values) {
    return Status.NOT_IMPLEMENTED;
  }

  @Override
  public Status insert(String table, String key,
      Map<String, ByteIterator> values) {
    try {
      client.insert(table, toDocument(key, values));
      return Status.OK;
    } catch (RuntimeException e) {
      LOGGER.log(Level.WARNING, "ArcusDB insert failed for key: " + key, e);
      return Status.ERROR;
    }
  }

  @Override
  public Status delete(String table, String key) {
    return Status.NOT_IMPLEMENTED;
  }

  private static Document toDocument(
      String key, Map<String, ByteIterator> values) {
    Document document = new Document(ID_FIELD, key);
    for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
      document.append(entry.getKey(), entry.getValue().toString());
    }
    return document;
  }

  private static void copyFields(Document document, Set<String> fields,
      Map<String, ByteIterator> result) {
    if (fields == null) {
      for (Map.Entry<String, Object> entry : document.entrySet()) {
        if (!ID_FIELD.equals(entry.getKey())) {
          putResult(result, entry.getKey(), entry.getValue());
        }
      }
      return;
    }

    for (String field : fields) {
      if (document.containsKey(field)) {
        putResult(result, field, document.get(field));
      }
    }
  }

  private static void putResult(
      Map<String, ByteIterator> result, String field, Object value) {
    if (value != null) {
      result.put(field, new StringByteIterator(String.valueOf(value)));
    }
  }
}
