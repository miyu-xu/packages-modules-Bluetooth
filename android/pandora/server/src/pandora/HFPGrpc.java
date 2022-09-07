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
 * <pre>
 * Service to trigger HFP (Hands Free Profile) procedures.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: pandora/hfp.proto")
public final class HFPGrpc {

  private HFPGrpc() {}

  public static final String SERVICE_NAME = "pandora.HFP";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<pandora.HfpProto.EnableSlcRequest,
      com.google.protobuf.Empty> getEnableSlcMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "EnableSlc",
      requestType = pandora.HfpProto.EnableSlcRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.HfpProto.EnableSlcRequest,
      com.google.protobuf.Empty> getEnableSlcMethod() {
    io.grpc.MethodDescriptor<pandora.HfpProto.EnableSlcRequest, com.google.protobuf.Empty> getEnableSlcMethod;
    if ((getEnableSlcMethod = HFPGrpc.getEnableSlcMethod) == null) {
      synchronized (HFPGrpc.class) {
        if ((getEnableSlcMethod = HFPGrpc.getEnableSlcMethod) == null) {
          HFPGrpc.getEnableSlcMethod = getEnableSlcMethod =
              io.grpc.MethodDescriptor.<pandora.HfpProto.EnableSlcRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "EnableSlc"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HfpProto.EnableSlcRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new HFPMethodDescriptorSupplier("EnableSlc"))
              .build();
        }
      }
    }
    return getEnableSlcMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.HfpProto.DisableSlcRequest,
      com.google.protobuf.Empty> getDisableSlcMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DisableSlc",
      requestType = pandora.HfpProto.DisableSlcRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.HfpProto.DisableSlcRequest,
      com.google.protobuf.Empty> getDisableSlcMethod() {
    io.grpc.MethodDescriptor<pandora.HfpProto.DisableSlcRequest, com.google.protobuf.Empty> getDisableSlcMethod;
    if ((getDisableSlcMethod = HFPGrpc.getDisableSlcMethod) == null) {
      synchronized (HFPGrpc.class) {
        if ((getDisableSlcMethod = HFPGrpc.getDisableSlcMethod) == null) {
          HFPGrpc.getDisableSlcMethod = getDisableSlcMethod =
              io.grpc.MethodDescriptor.<pandora.HfpProto.DisableSlcRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DisableSlc"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HfpProto.DisableSlcRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new HFPMethodDescriptorSupplier("DisableSlc"))
              .build();
        }
      }
    }
    return getDisableSlcMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static HFPStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HFPStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HFPStub>() {
        @java.lang.Override
        public HFPStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HFPStub(channel, callOptions);
        }
      };
    return HFPStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static HFPBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HFPBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HFPBlockingStub>() {
        @java.lang.Override
        public HFPBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HFPBlockingStub(channel, callOptions);
        }
      };
    return HFPBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static HFPFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HFPFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HFPFutureStub>() {
        @java.lang.Override
        public HFPFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HFPFutureStub(channel, callOptions);
        }
      };
    return HFPFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Service to trigger HFP (Hands Free Profile) procedures.
   * </pre>
   */
  public static abstract class HFPImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Enable Service level connection
     * </pre>
     */
    public void enableSlc(pandora.HfpProto.EnableSlcRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getEnableSlcMethod(), responseObserver);
    }

    /**
     * <pre>
     * Disable Service level connection
     * </pre>
     */
    public void disableSlc(pandora.HfpProto.DisableSlcRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDisableSlcMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getEnableSlcMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.HfpProto.EnableSlcRequest,
                com.google.protobuf.Empty>(
                  this, METHODID_ENABLE_SLC)))
          .addMethod(
            getDisableSlcMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.HfpProto.DisableSlcRequest,
                com.google.protobuf.Empty>(
                  this, METHODID_DISABLE_SLC)))
          .build();
    }
  }

  /**
   * <pre>
   * Service to trigger HFP (Hands Free Profile) procedures.
   * </pre>
   */
  public static final class HFPStub extends io.grpc.stub.AbstractAsyncStub<HFPStub> {
    private HFPStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HFPStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HFPStub(channel, callOptions);
    }

    /**
     * <pre>
     * Enable Service level connection
     * </pre>
     */
    public void enableSlc(pandora.HfpProto.EnableSlcRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getEnableSlcMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Disable Service level connection
     * </pre>
     */
    public void disableSlc(pandora.HfpProto.DisableSlcRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDisableSlcMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Service to trigger HFP (Hands Free Profile) procedures.
   * </pre>
   */
  public static final class HFPBlockingStub extends io.grpc.stub.AbstractBlockingStub<HFPBlockingStub> {
    private HFPBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HFPBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HFPBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Enable Service level connection
     * </pre>
     */
    public com.google.protobuf.Empty enableSlc(pandora.HfpProto.EnableSlcRequest request) {
      return blockingUnaryCall(
          getChannel(), getEnableSlcMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Disable Service level connection
     * </pre>
     */
    public com.google.protobuf.Empty disableSlc(pandora.HfpProto.DisableSlcRequest request) {
      return blockingUnaryCall(
          getChannel(), getDisableSlcMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Service to trigger HFP (Hands Free Profile) procedures.
   * </pre>
   */
  public static final class HFPFutureStub extends io.grpc.stub.AbstractFutureStub<HFPFutureStub> {
    private HFPFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HFPFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HFPFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Enable Service level connection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> enableSlc(
        pandora.HfpProto.EnableSlcRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getEnableSlcMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Disable Service level connection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> disableSlc(
        pandora.HfpProto.DisableSlcRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDisableSlcMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ENABLE_SLC = 0;
  private static final int METHODID_DISABLE_SLC = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final HFPImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(HFPImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_ENABLE_SLC:
          serviceImpl.enableSlc((pandora.HfpProto.EnableSlcRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DISABLE_SLC:
          serviceImpl.disableSlc((pandora.HfpProto.DisableSlcRequest) request,
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

  private static abstract class HFPBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    HFPBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return pandora.HfpProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("HFP");
    }
  }

  private static final class HFPFileDescriptorSupplier
      extends HFPBaseDescriptorSupplier {
    HFPFileDescriptorSupplier() {}
  }

  private static final class HFPMethodDescriptorSupplier
      extends HFPBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    HFPMethodDescriptorSupplier(String methodName) {
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
      synchronized (HFPGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new HFPFileDescriptorSupplier())
              .addMethod(getEnableSlcMethod())
              .addMethod(getDisableSlcMethod())
              .build();
        }
      }
    }
    return result;
  }
}
