package com.jam2in.arcusdb.client;

import com.jam2in.arcusdb.proto.ArcusDbProto.Request;
import com.jam2in.arcusdb.proto.ArcusDbProto.Response;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Framed socket connection that matches ArcusDB responses to requests.
 */
public final class ArcusDbConnection implements AutoCloseable {

  private final ArcusDbClientConfig config;
  private final Socket socket;
  private final Object writeLock = new Object();
  private final AtomicInteger nextRequestId = new AtomicInteger(1);
  private final Map<Integer, CompletableFuture<Response>> pending = new ConcurrentHashMap<>();
  private final AtomicBoolean closed = new AtomicBoolean();
  private final Thread readerThread;

  public ArcusDbConnection(ArcusDbClientConfig config) {
    this.config = config;
    this.socket = connect(config);
    this.readerThread = new Thread(this::readResponses, "arcusdb-client-reader");
    this.readerThread.setDaemon(true);
    this.readerThread.start();
  }

  public Response send(Request requestWithoutId) {
    ensureOpen();
    CompletableFuture<Response> future = new CompletableFuture<>();
    int requestId = reserveRequestId(future);
    Request request = requestWithoutId.toBuilder().setRequestId(requestId).build();
    try {
      synchronized (writeLock) {
        FrameCodec.writeFrame(socket.getOutputStream(), request.toByteArray());
      }
      return awaitResponse(requestId, future);
    } catch (IOException e) {
      pending.remove(requestId, future);
      future.completeExceptionally(e);
      throw new ArcusDbException("failed to write request", e);
    }
  }

  @Override
  public void close() {
    close(new ArcusDbException("ArcusDB client closed"));
  }

  private Response awaitResponse(int requestId, CompletableFuture<Response> future) {
    try {
      return future.get(config.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      pending.remove(requestId, future);
      throw new ArcusDbException("request timed out: " + requestId, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      pending.remove(requestId, future);
      throw new ArcusDbException("interrupted while waiting for response", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof ArcusDbException) {
        throw (ArcusDbException) cause;
      }
      throw new ArcusDbException("request failed", cause);
    }
  }

  private void readResponses() {
    try {
      while (!closed.get()) {
        byte[] payload = FrameCodec.readFrame(socket.getInputStream());
        Response response = Response.parseFrom(payload);
        CompletableFuture<Response> future = pending.remove(response.getResponseTo());
        if (future != null) {
          future.complete(response);
        }
      }
    } catch (EOFException e) {
      if (!closed.get()) {
        close(new ArcusDbException("connection closed by server", e));
      }
    } catch (Exception e) {
      if (!closed.get()) {
        close(new ArcusDbException("failed to read response", e));
      }
    }
  }

  private int reserveRequestId(CompletableFuture<Response> future) {
    while (true) {
      int requestId = nextRequestId.getAndUpdate(
          current -> current == Integer.MAX_VALUE
              ? 1
              : current + 1);

      ensureOpen();
      if (pending.putIfAbsent(requestId, future) == null) {
        return requestId;
      }
    }
  }

  private void close(ArcusDbException exception) {
    if (!closed.compareAndSet(false, true)) {
      return;
    }

    try {
      socket.close();
    } catch (IOException ignored) {
      // Closing is best-effort; pending calls are failed below.
    }

    failPending(exception);
    if (Thread.currentThread() != readerThread) {
      try {
        readerThread.join(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void failPending(ArcusDbException exception) {
    for (CompletableFuture<Response> future : pending.values()) {
      future.completeExceptionally(exception);
    }

    pending.clear();
  }

  private void ensureOpen() {
    if (closed.get() || socket.isClosed()) {
      throw new ArcusDbException("ArcusDB client is closed");
    }
  }

  private static Socket connect(ArcusDbClientConfig config) {
    Socket socket = new Socket();
    try {
      socket.setTcpNoDelay(true);
      SocketAddress address = new InetSocketAddress(config.getHost(), config.getPort());
      socket.connect(address, config.getTimeoutMs());
      return socket;
    } catch (IOException e) {
      closeQuietly(socket);
      throw new ArcusDbException(
          "failed to connect to ArcusDB at " + config.getHost() + ":" + config.getPort(), e);
    }
  }

  private static void closeQuietly(Socket socket) {
    try {
      socket.close();
    } catch (IOException ignore) {
      // Ignore close failure after connect failure.
    }
  }
}
