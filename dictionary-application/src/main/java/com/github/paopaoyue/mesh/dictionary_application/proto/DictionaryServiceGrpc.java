package com.github.paopaoyue.mesh.dictionary_application.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.9.1)",
    comments = "Source: dictionary-application/dictionary.proto")
public final class DictionaryServiceGrpc {

  private DictionaryServiceGrpc() {}

  public static final String SERVICE_NAME = "rpc.DictionaryService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetRequest,
      com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetResponse> METHOD_GET = getGetMethod();

  private static volatile io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetRequest,
      com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetResponse> getGetMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetRequest,
      com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetResponse> getGetMethod() {
    io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetRequest, com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetResponse> getGetMethod;
    if ((getGetMethod = DictionaryServiceGrpc.getGetMethod) == null) {
      synchronized (DictionaryServiceGrpc.class) {
        if ((getGetMethod = DictionaryServiceGrpc.getGetMethod) == null) {
          DictionaryServiceGrpc.getGetMethod = getGetMethod = 
              io.grpc.MethodDescriptor.<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetRequest, com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "rpc.DictionaryService", "Get"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DictionaryServiceMethodDescriptorSupplier("Get"))
                  .build();
          }
        }
     }
     return getGetMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getAddMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddRequest,
      com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddResponse> METHOD_ADD = getAddMethod();

  private static volatile io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddRequest,
      com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddResponse> getAddMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddRequest,
      com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddResponse> getAddMethod() {
    io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddRequest, com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddResponse> getAddMethod;
    if ((getAddMethod = DictionaryServiceGrpc.getAddMethod) == null) {
      synchronized (DictionaryServiceGrpc.class) {
        if ((getAddMethod = DictionaryServiceGrpc.getAddMethod) == null) {
          DictionaryServiceGrpc.getAddMethod = getAddMethod = 
              io.grpc.MethodDescriptor.<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddRequest, com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "rpc.DictionaryService", "Add"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DictionaryServiceMethodDescriptorSupplier("Add"))
                  .build();
          }
        }
     }
     return getAddMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUpdateMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateRequest,
      com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateResponse> METHOD_UPDATE = getUpdateMethod();

  private static volatile io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateRequest,
      com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateResponse> getUpdateMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateRequest,
      com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateResponse> getUpdateMethod() {
    io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateRequest, com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateResponse> getUpdateMethod;
    if ((getUpdateMethod = DictionaryServiceGrpc.getUpdateMethod) == null) {
      synchronized (DictionaryServiceGrpc.class) {
        if ((getUpdateMethod = DictionaryServiceGrpc.getUpdateMethod) == null) {
          DictionaryServiceGrpc.getUpdateMethod = getUpdateMethod = 
              io.grpc.MethodDescriptor.<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateRequest, com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "rpc.DictionaryService", "Update"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DictionaryServiceMethodDescriptorSupplier("Update"))
                  .build();
          }
        }
     }
     return getUpdateMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getRemoveMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveRequest,
      com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveResponse> METHOD_REMOVE = getRemoveMethod();

  private static volatile io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveRequest,
      com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveResponse> getRemoveMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveRequest,
      com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveResponse> getRemoveMethod() {
    io.grpc.MethodDescriptor<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveRequest, com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveResponse> getRemoveMethod;
    if ((getRemoveMethod = DictionaryServiceGrpc.getRemoveMethod) == null) {
      synchronized (DictionaryServiceGrpc.class) {
        if ((getRemoveMethod = DictionaryServiceGrpc.getRemoveMethod) == null) {
          DictionaryServiceGrpc.getRemoveMethod = getRemoveMethod = 
              io.grpc.MethodDescriptor.<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveRequest, com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "rpc.DictionaryService", "Remove"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DictionaryServiceMethodDescriptorSupplier("Remove"))
                  .build();
          }
        }
     }
     return getRemoveMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DictionaryServiceStub newStub(io.grpc.Channel channel) {
    return new DictionaryServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DictionaryServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new DictionaryServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static DictionaryServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new DictionaryServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class DictionaryServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void get(com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetRequest request,
        io.grpc.stub.StreamObserver<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetMethod(), responseObserver);
    }

    /**
     */
    public void add(com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddRequest request,
        io.grpc.stub.StreamObserver<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAddMethod(), responseObserver);
    }

    /**
     */
    public void update(com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateRequest request,
        io.grpc.stub.StreamObserver<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateMethod(), responseObserver);
    }

    /**
     */
    public void remove(com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveRequest request,
        io.grpc.stub.StreamObserver<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRemoveMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetRequest,
                com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetResponse>(
                  this, METHODID_GET)))
          .addMethod(
            getAddMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddRequest,
                com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddResponse>(
                  this, METHODID_ADD)))
          .addMethod(
            getUpdateMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateRequest,
                com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateResponse>(
                  this, METHODID_UPDATE)))
          .addMethod(
            getRemoveMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveRequest,
                com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveResponse>(
                  this, METHODID_REMOVE)))
          .build();
    }
  }

  /**
   */
  public static final class DictionaryServiceStub extends io.grpc.stub.AbstractStub<DictionaryServiceStub> {
    private DictionaryServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DictionaryServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DictionaryServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DictionaryServiceStub(channel, callOptions);
    }

    /**
     */
    public void get(com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetRequest request,
        io.grpc.stub.StreamObserver<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void add(com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddRequest request,
        io.grpc.stub.StreamObserver<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAddMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void update(com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateRequest request,
        io.grpc.stub.StreamObserver<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void remove(com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveRequest request,
        io.grpc.stub.StreamObserver<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRemoveMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class DictionaryServiceBlockingStub extends io.grpc.stub.AbstractStub<DictionaryServiceBlockingStub> {
    private DictionaryServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DictionaryServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DictionaryServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DictionaryServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetResponse get(com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddResponse add(com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddRequest request) {
      return blockingUnaryCall(
          getChannel(), getAddMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateResponse update(com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateRequest request) {
      return blockingUnaryCall(
          getChannel(), getUpdateMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveResponse remove(com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveRequest request) {
      return blockingUnaryCall(
          getChannel(), getRemoveMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class DictionaryServiceFutureStub extends io.grpc.stub.AbstractStub<DictionaryServiceFutureStub> {
    private DictionaryServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DictionaryServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DictionaryServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DictionaryServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetResponse> get(
        com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddResponse> add(
        com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAddMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateResponse> update(
        com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveResponse> remove(
        com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRemoveMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET = 0;
  private static final int METHODID_ADD = 1;
  private static final int METHODID_UPDATE = 2;
  private static final int METHODID_REMOVE = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final DictionaryServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(DictionaryServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET:
          serviceImpl.get((com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetRequest) request,
              (io.grpc.stub.StreamObserver<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.GetResponse>) responseObserver);
          break;
        case METHODID_ADD:
          serviceImpl.add((com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddRequest) request,
              (io.grpc.stub.StreamObserver<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.AddResponse>) responseObserver);
          break;
        case METHODID_UPDATE:
          serviceImpl.update((com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateRequest) request,
              (io.grpc.stub.StreamObserver<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.UpdateResponse>) responseObserver);
          break;
        case METHODID_REMOVE:
          serviceImpl.remove((com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveRequest) request,
              (io.grpc.stub.StreamObserver<com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.RemoveResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class DictionaryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    DictionaryServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("DictionaryService");
    }
  }

  private static final class DictionaryServiceFileDescriptorSupplier
      extends DictionaryServiceBaseDescriptorSupplier {
    DictionaryServiceFileDescriptorSupplier() {}
  }

  private static final class DictionaryServiceMethodDescriptorSupplier
      extends DictionaryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    DictionaryServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (DictionaryServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new DictionaryServiceFileDescriptorSupplier())
              .addMethod(getGetMethod())
              .addMethod(getAddMethod())
              .addMethod(getUpdateMethod())
              .addMethod(getRemoveMethod())
              .build();
        }
      }
    }
    return result;
  }
}
