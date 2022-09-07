package pandora;

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
    value = "by gRPC proto compiler",
    comments = "Source: pandora/gatt.proto")
public final class GATTGrpc {

  private GATTGrpc() {}

  public static final String SERVICE_NAME = "pandora.GATT";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<pandora.GattProto.ExchangeMTURequest,
      com.google.protobuf.Empty> getExchangeMTUMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExchangeMTU",
      requestType = pandora.GattProto.ExchangeMTURequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.GattProto.ExchangeMTURequest,
      com.google.protobuf.Empty> getExchangeMTUMethod() {
    io.grpc.MethodDescriptor<pandora.GattProto.ExchangeMTURequest, com.google.protobuf.Empty> getExchangeMTUMethod;
    if ((getExchangeMTUMethod = GATTGrpc.getExchangeMTUMethod) == null) {
      synchronized (GATTGrpc.class) {
        if ((getExchangeMTUMethod = GATTGrpc.getExchangeMTUMethod) == null) {
          GATTGrpc.getExchangeMTUMethod = getExchangeMTUMethod =
              io.grpc.MethodDescriptor.<pandora.GattProto.ExchangeMTURequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExchangeMTU"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.GattProto.ExchangeMTURequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new GATTMethodDescriptorSupplier("ExchangeMTU"))
              .build();
        }
      }
    }
    return getExchangeMTUMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.GattProto.WriteCharacteristicRequest,
      com.google.protobuf.Empty> getWriteCharacteristicFromHandleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WriteCharacteristicFromHandle",
      requestType = pandora.GattProto.WriteCharacteristicRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.GattProto.WriteCharacteristicRequest,
      com.google.protobuf.Empty> getWriteCharacteristicFromHandleMethod() {
    io.grpc.MethodDescriptor<pandora.GattProto.WriteCharacteristicRequest, com.google.protobuf.Empty> getWriteCharacteristicFromHandleMethod;
    if ((getWriteCharacteristicFromHandleMethod = GATTGrpc.getWriteCharacteristicFromHandleMethod) == null) {
      synchronized (GATTGrpc.class) {
        if ((getWriteCharacteristicFromHandleMethod = GATTGrpc.getWriteCharacteristicFromHandleMethod) == null) {
          GATTGrpc.getWriteCharacteristicFromHandleMethod = getWriteCharacteristicFromHandleMethod =
              io.grpc.MethodDescriptor.<pandora.GattProto.WriteCharacteristicRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WriteCharacteristicFromHandle"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.GattProto.WriteCharacteristicRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new GATTMethodDescriptorSupplier("WriteCharacteristicFromHandle"))
              .build();
        }
      }
    }
    return getWriteCharacteristicFromHandleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.GattProto.DiscoverServiceByUuidRequest,
      pandora.GattProto.DiscoverServicesResponse> getDiscoverServiceByUuidMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DiscoverServiceByUuid",
      requestType = pandora.GattProto.DiscoverServiceByUuidRequest.class,
      responseType = pandora.GattProto.DiscoverServicesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.GattProto.DiscoverServiceByUuidRequest,
      pandora.GattProto.DiscoverServicesResponse> getDiscoverServiceByUuidMethod() {
    io.grpc.MethodDescriptor<pandora.GattProto.DiscoverServiceByUuidRequest, pandora.GattProto.DiscoverServicesResponse> getDiscoverServiceByUuidMethod;
    if ((getDiscoverServiceByUuidMethod = GATTGrpc.getDiscoverServiceByUuidMethod) == null) {
      synchronized (GATTGrpc.class) {
        if ((getDiscoverServiceByUuidMethod = GATTGrpc.getDiscoverServiceByUuidMethod) == null) {
          GATTGrpc.getDiscoverServiceByUuidMethod = getDiscoverServiceByUuidMethod =
              io.grpc.MethodDescriptor.<pandora.GattProto.DiscoverServiceByUuidRequest, pandora.GattProto.DiscoverServicesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DiscoverServiceByUuid"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.GattProto.DiscoverServiceByUuidRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.GattProto.DiscoverServicesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GATTMethodDescriptorSupplier("DiscoverServiceByUuid"))
              .build();
        }
      }
    }
    return getDiscoverServiceByUuidMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.GattProto.DiscoverServicesRequest,
      pandora.GattProto.DiscoverServicesResponse> getDiscoverServicesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DiscoverServices",
      requestType = pandora.GattProto.DiscoverServicesRequest.class,
      responseType = pandora.GattProto.DiscoverServicesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.GattProto.DiscoverServicesRequest,
      pandora.GattProto.DiscoverServicesResponse> getDiscoverServicesMethod() {
    io.grpc.MethodDescriptor<pandora.GattProto.DiscoverServicesRequest, pandora.GattProto.DiscoverServicesResponse> getDiscoverServicesMethod;
    if ((getDiscoverServicesMethod = GATTGrpc.getDiscoverServicesMethod) == null) {
      synchronized (GATTGrpc.class) {
        if ((getDiscoverServicesMethod = GATTGrpc.getDiscoverServicesMethod) == null) {
          GATTGrpc.getDiscoverServicesMethod = getDiscoverServicesMethod =
              io.grpc.MethodDescriptor.<pandora.GattProto.DiscoverServicesRequest, pandora.GattProto.DiscoverServicesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DiscoverServices"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.GattProto.DiscoverServicesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.GattProto.DiscoverServicesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GATTMethodDescriptorSupplier("DiscoverServices"))
              .build();
        }
      }
    }
    return getDiscoverServicesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.GattProto.DiscoverServicesSdpRequest,
      pandora.GattProto.DiscoverServicesSdpResponse> getDiscoverServicesSdpMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DiscoverServicesSdp",
      requestType = pandora.GattProto.DiscoverServicesSdpRequest.class,
      responseType = pandora.GattProto.DiscoverServicesSdpResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.GattProto.DiscoverServicesSdpRequest,
      pandora.GattProto.DiscoverServicesSdpResponse> getDiscoverServicesSdpMethod() {
    io.grpc.MethodDescriptor<pandora.GattProto.DiscoverServicesSdpRequest, pandora.GattProto.DiscoverServicesSdpResponse> getDiscoverServicesSdpMethod;
    if ((getDiscoverServicesSdpMethod = GATTGrpc.getDiscoverServicesSdpMethod) == null) {
      synchronized (GATTGrpc.class) {
        if ((getDiscoverServicesSdpMethod = GATTGrpc.getDiscoverServicesSdpMethod) == null) {
          GATTGrpc.getDiscoverServicesSdpMethod = getDiscoverServicesSdpMethod =
              io.grpc.MethodDescriptor.<pandora.GattProto.DiscoverServicesSdpRequest, pandora.GattProto.DiscoverServicesSdpResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DiscoverServicesSdp"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.GattProto.DiscoverServicesSdpRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.GattProto.DiscoverServicesSdpResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GATTMethodDescriptorSupplier("DiscoverServicesSdp"))
              .build();
        }
      }
    }
    return getDiscoverServicesSdpMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.GattProto.ClearCacheRequest,
      com.google.protobuf.Empty> getClearCacheMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ClearCache",
      requestType = pandora.GattProto.ClearCacheRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.GattProto.ClearCacheRequest,
      com.google.protobuf.Empty> getClearCacheMethod() {
    io.grpc.MethodDescriptor<pandora.GattProto.ClearCacheRequest, com.google.protobuf.Empty> getClearCacheMethod;
    if ((getClearCacheMethod = GATTGrpc.getClearCacheMethod) == null) {
      synchronized (GATTGrpc.class) {
        if ((getClearCacheMethod = GATTGrpc.getClearCacheMethod) == null) {
          GATTGrpc.getClearCacheMethod = getClearCacheMethod =
              io.grpc.MethodDescriptor.<pandora.GattProto.ClearCacheRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ClearCache"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.GattProto.ClearCacheRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new GATTMethodDescriptorSupplier("ClearCache"))
              .build();
        }
      }
    }
    return getClearCacheMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static GATTStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GATTStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GATTStub>() {
        @java.lang.Override
        public GATTStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GATTStub(channel, callOptions);
        }
      };
    return GATTStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static GATTBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GATTBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GATTBlockingStub>() {
        @java.lang.Override
        public GATTBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GATTBlockingStub(channel, callOptions);
        }
      };
    return GATTBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static GATTFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GATTFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GATTFutureStub>() {
        @java.lang.Override
        public GATTFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GATTFutureStub(channel, callOptions);
        }
      };
    return GATTFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class GATTImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Request an MTU size.
     * </pre>
     */
    public void exchangeMTU(pandora.GattProto.ExchangeMTURequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getExchangeMTUMethod(), responseObserver);
    }

    /**
     * <pre>
     * Writes on a characteristic.
     * </pre>
     */
    public void writeCharacteristicFromHandle(pandora.GattProto.WriteCharacteristicRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getWriteCharacteristicFromHandleMethod(), responseObserver);
    }

    /**
     * <pre>
     * Starts service discovery for given uuid.
     * </pre>
     */
    public void discoverServiceByUuid(pandora.GattProto.DiscoverServiceByUuidRequest request,
        io.grpc.stub.StreamObserver<pandora.GattProto.DiscoverServicesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDiscoverServiceByUuidMethod(), responseObserver);
    }

    /**
     * <pre>
     * Starts services discovery.
     * </pre>
     */
    public void discoverServices(pandora.GattProto.DiscoverServicesRequest request,
        io.grpc.stub.StreamObserver<pandora.GattProto.DiscoverServicesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDiscoverServicesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Starts services discovery using SDP.
     * </pre>
     */
    public void discoverServicesSdp(pandora.GattProto.DiscoverServicesSdpRequest request,
        io.grpc.stub.StreamObserver<pandora.GattProto.DiscoverServicesSdpResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDiscoverServicesSdpMethod(), responseObserver);
    }

    /**
     * <pre>
     * Clears DUT GATT cache.
     * </pre>
     */
    public void clearCache(pandora.GattProto.ClearCacheRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getClearCacheMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getExchangeMTUMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.GattProto.ExchangeMTURequest,
                com.google.protobuf.Empty>(
                  this, METHODID_EXCHANGE_MTU)))
          .addMethod(
            getWriteCharacteristicFromHandleMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.GattProto.WriteCharacteristicRequest,
                com.google.protobuf.Empty>(
                  this, METHODID_WRITE_CHARACTERISTIC_FROM_HANDLE)))
          .addMethod(
            getDiscoverServiceByUuidMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.GattProto.DiscoverServiceByUuidRequest,
                pandora.GattProto.DiscoverServicesResponse>(
                  this, METHODID_DISCOVER_SERVICE_BY_UUID)))
          .addMethod(
            getDiscoverServicesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.GattProto.DiscoverServicesRequest,
                pandora.GattProto.DiscoverServicesResponse>(
                  this, METHODID_DISCOVER_SERVICES)))
          .addMethod(
            getDiscoverServicesSdpMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.GattProto.DiscoverServicesSdpRequest,
                pandora.GattProto.DiscoverServicesSdpResponse>(
                  this, METHODID_DISCOVER_SERVICES_SDP)))
          .addMethod(
            getClearCacheMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.GattProto.ClearCacheRequest,
                com.google.protobuf.Empty>(
                  this, METHODID_CLEAR_CACHE)))
          .build();
    }
  }

  /**
   */
  public static final class GATTStub extends io.grpc.stub.AbstractAsyncStub<GATTStub> {
    private GATTStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GATTStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GATTStub(channel, callOptions);
    }

    /**
     * <pre>
     * Request an MTU size.
     * </pre>
     */
    public void exchangeMTU(pandora.GattProto.ExchangeMTURequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExchangeMTUMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Writes on a characteristic.
     * </pre>
     */
    public void writeCharacteristicFromHandle(pandora.GattProto.WriteCharacteristicRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWriteCharacteristicFromHandleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Starts service discovery for given uuid.
     * </pre>
     */
    public void discoverServiceByUuid(pandora.GattProto.DiscoverServiceByUuidRequest request,
        io.grpc.stub.StreamObserver<pandora.GattProto.DiscoverServicesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDiscoverServiceByUuidMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Starts services discovery.
     * </pre>
     */
    public void discoverServices(pandora.GattProto.DiscoverServicesRequest request,
        io.grpc.stub.StreamObserver<pandora.GattProto.DiscoverServicesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDiscoverServicesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Starts services discovery using SDP.
     * </pre>
     */
    public void discoverServicesSdp(pandora.GattProto.DiscoverServicesSdpRequest request,
        io.grpc.stub.StreamObserver<pandora.GattProto.DiscoverServicesSdpResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDiscoverServicesSdpMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Clears DUT GATT cache.
     * </pre>
     */
    public void clearCache(pandora.GattProto.ClearCacheRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getClearCacheMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class GATTBlockingStub extends io.grpc.stub.AbstractBlockingStub<GATTBlockingStub> {
    private GATTBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GATTBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GATTBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Request an MTU size.
     * </pre>
     */
    public com.google.protobuf.Empty exchangeMTU(pandora.GattProto.ExchangeMTURequest request) {
      return blockingUnaryCall(
          getChannel(), getExchangeMTUMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Writes on a characteristic.
     * </pre>
     */
    public com.google.protobuf.Empty writeCharacteristicFromHandle(pandora.GattProto.WriteCharacteristicRequest request) {
      return blockingUnaryCall(
          getChannel(), getWriteCharacteristicFromHandleMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Starts service discovery for given uuid.
     * </pre>
     */
    public pandora.GattProto.DiscoverServicesResponse discoverServiceByUuid(pandora.GattProto.DiscoverServiceByUuidRequest request) {
      return blockingUnaryCall(
          getChannel(), getDiscoverServiceByUuidMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Starts services discovery.
     * </pre>
     */
    public pandora.GattProto.DiscoverServicesResponse discoverServices(pandora.GattProto.DiscoverServicesRequest request) {
      return blockingUnaryCall(
          getChannel(), getDiscoverServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Starts services discovery using SDP.
     * </pre>
     */
    public pandora.GattProto.DiscoverServicesSdpResponse discoverServicesSdp(pandora.GattProto.DiscoverServicesSdpRequest request) {
      return blockingUnaryCall(
          getChannel(), getDiscoverServicesSdpMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Clears DUT GATT cache.
     * </pre>
     */
    public com.google.protobuf.Empty clearCache(pandora.GattProto.ClearCacheRequest request) {
      return blockingUnaryCall(
          getChannel(), getClearCacheMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class GATTFutureStub extends io.grpc.stub.AbstractFutureStub<GATTFutureStub> {
    private GATTFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GATTFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GATTFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Request an MTU size.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> exchangeMTU(
        pandora.GattProto.ExchangeMTURequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExchangeMTUMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Writes on a characteristic.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> writeCharacteristicFromHandle(
        pandora.GattProto.WriteCharacteristicRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getWriteCharacteristicFromHandleMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Starts service discovery for given uuid.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.GattProto.DiscoverServicesResponse> discoverServiceByUuid(
        pandora.GattProto.DiscoverServiceByUuidRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDiscoverServiceByUuidMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Starts services discovery.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.GattProto.DiscoverServicesResponse> discoverServices(
        pandora.GattProto.DiscoverServicesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDiscoverServicesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Starts services discovery using SDP.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.GattProto.DiscoverServicesSdpResponse> discoverServicesSdp(
        pandora.GattProto.DiscoverServicesSdpRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDiscoverServicesSdpMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Clears DUT GATT cache.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> clearCache(
        pandora.GattProto.ClearCacheRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getClearCacheMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_EXCHANGE_MTU = 0;
  private static final int METHODID_WRITE_CHARACTERISTIC_FROM_HANDLE = 1;
  private static final int METHODID_DISCOVER_SERVICE_BY_UUID = 2;
  private static final int METHODID_DISCOVER_SERVICES = 3;
  private static final int METHODID_DISCOVER_SERVICES_SDP = 4;
  private static final int METHODID_CLEAR_CACHE = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final GATTImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(GATTImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_EXCHANGE_MTU:
          serviceImpl.exchangeMTU((pandora.GattProto.ExchangeMTURequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_WRITE_CHARACTERISTIC_FROM_HANDLE:
          serviceImpl.writeCharacteristicFromHandle((pandora.GattProto.WriteCharacteristicRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DISCOVER_SERVICE_BY_UUID:
          serviceImpl.discoverServiceByUuid((pandora.GattProto.DiscoverServiceByUuidRequest) request,
              (io.grpc.stub.StreamObserver<pandora.GattProto.DiscoverServicesResponse>) responseObserver);
          break;
        case METHODID_DISCOVER_SERVICES:
          serviceImpl.discoverServices((pandora.GattProto.DiscoverServicesRequest) request,
              (io.grpc.stub.StreamObserver<pandora.GattProto.DiscoverServicesResponse>) responseObserver);
          break;
        case METHODID_DISCOVER_SERVICES_SDP:
          serviceImpl.discoverServicesSdp((pandora.GattProto.DiscoverServicesSdpRequest) request,
              (io.grpc.stub.StreamObserver<pandora.GattProto.DiscoverServicesSdpResponse>) responseObserver);
          break;
        case METHODID_CLEAR_CACHE:
          serviceImpl.clearCache((pandora.GattProto.ClearCacheRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
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

  private static abstract class GATTBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    GATTBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return pandora.GattProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("GATT");
    }
  }

  private static final class GATTFileDescriptorSupplier
      extends GATTBaseDescriptorSupplier {
    GATTFileDescriptorSupplier() {}
  }

  private static final class GATTMethodDescriptorSupplier
      extends GATTBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    GATTMethodDescriptorSupplier(String methodName) {
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
      synchronized (GATTGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new GATTFileDescriptorSupplier())
              .addMethod(getExchangeMTUMethod())
              .addMethod(getWriteCharacteristicFromHandleMethod())
              .addMethod(getDiscoverServiceByUuidMethod())
              .addMethod(getDiscoverServicesMethod())
              .addMethod(getDiscoverServicesSdpMethod())
              .addMethod(getClearCacheMethod())
              .build();
        }
      }
    }
    return result;
  }
}
