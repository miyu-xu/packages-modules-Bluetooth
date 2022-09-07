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
    comments = "Source: pandora/l2cap.proto")
public final class L2CAPGrpc {

  private L2CAPGrpc() {}

  public static final String SERVICE_NAME = "pandora.L2CAP";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getStartAdvertisementMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StartAdvertisement",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getStartAdvertisementMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getStartAdvertisementMethod;
    if ((getStartAdvertisementMethod = L2CAPGrpc.getStartAdvertisementMethod) == null) {
      synchronized (L2CAPGrpc.class) {
        if ((getStartAdvertisementMethod = L2CAPGrpc.getStartAdvertisementMethod) == null) {
          L2CAPGrpc.getStartAdvertisementMethod = getStartAdvertisementMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StartAdvertisement"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new L2CAPMethodDescriptorSupplier("StartAdvertisement"))
              .build();
        }
      }
    }
    return getStartAdvertisementMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.L2capProto.MakeConnectionRequest,
      pandora.L2capProto.MakeConnectionResponse> getMakeConnectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MakeConnection",
      requestType = pandora.L2capProto.MakeConnectionRequest.class,
      responseType = pandora.L2capProto.MakeConnectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.L2capProto.MakeConnectionRequest,
      pandora.L2capProto.MakeConnectionResponse> getMakeConnectionMethod() {
    io.grpc.MethodDescriptor<pandora.L2capProto.MakeConnectionRequest, pandora.L2capProto.MakeConnectionResponse> getMakeConnectionMethod;
    if ((getMakeConnectionMethod = L2CAPGrpc.getMakeConnectionMethod) == null) {
      synchronized (L2CAPGrpc.class) {
        if ((getMakeConnectionMethod = L2CAPGrpc.getMakeConnectionMethod) == null) {
          L2CAPGrpc.getMakeConnectionMethod = getMakeConnectionMethod =
              io.grpc.MethodDescriptor.<pandora.L2capProto.MakeConnectionRequest, pandora.L2capProto.MakeConnectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MakeConnection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.L2capProto.MakeConnectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.L2capProto.MakeConnectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new L2CAPMethodDescriptorSupplier("MakeConnection"))
              .build();
        }
      }
    }
    return getMakeConnectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.L2capProto.SendLEDataPacketRequest,
      pandora.L2capProto.SendLEDataPacketResponse> getSendLEDataPacketMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendLEDataPacket",
      requestType = pandora.L2capProto.SendLEDataPacketRequest.class,
      responseType = pandora.L2capProto.SendLEDataPacketResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.L2capProto.SendLEDataPacketRequest,
      pandora.L2capProto.SendLEDataPacketResponse> getSendLEDataPacketMethod() {
    io.grpc.MethodDescriptor<pandora.L2capProto.SendLEDataPacketRequest, pandora.L2capProto.SendLEDataPacketResponse> getSendLEDataPacketMethod;
    if ((getSendLEDataPacketMethod = L2CAPGrpc.getSendLEDataPacketMethod) == null) {
      synchronized (L2CAPGrpc.class) {
        if ((getSendLEDataPacketMethod = L2CAPGrpc.getSendLEDataPacketMethod) == null) {
          L2CAPGrpc.getSendLEDataPacketMethod = getSendLEDataPacketMethod =
              io.grpc.MethodDescriptor.<pandora.L2capProto.SendLEDataPacketRequest, pandora.L2capProto.SendLEDataPacketResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendLEDataPacket"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.L2capProto.SendLEDataPacketRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.L2capProto.SendLEDataPacketResponse.getDefaultInstance()))
              .setSchemaDescriptor(new L2CAPMethodDescriptorSupplier("SendLEDataPacket"))
              .build();
        }
      }
    }
    return getSendLEDataPacketMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static L2CAPStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<L2CAPStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<L2CAPStub>() {
        @java.lang.Override
        public L2CAPStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new L2CAPStub(channel, callOptions);
        }
      };
    return L2CAPStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static L2CAPBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<L2CAPBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<L2CAPBlockingStub>() {
        @java.lang.Override
        public L2CAPBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new L2CAPBlockingStub(channel, callOptions);
        }
      };
    return L2CAPBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static L2CAPFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<L2CAPFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<L2CAPFutureStub>() {
        @java.lang.Override
        public L2CAPFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new L2CAPFutureStub(channel, callOptions);
        }
      };
    return L2CAPFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class L2CAPImplBase implements io.grpc.BindableService {

    /**
     */
    public void startAdvertisement(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getStartAdvertisementMethod(), responseObserver);
    }

    /**
     */
    public void makeConnection(pandora.L2capProto.MakeConnectionRequest request,
        io.grpc.stub.StreamObserver<pandora.L2capProto.MakeConnectionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getMakeConnectionMethod(), responseObserver);
    }

    /**
     */
    public void sendLEDataPacket(pandora.L2capProto.SendLEDataPacketRequest request,
        io.grpc.stub.StreamObserver<pandora.L2capProto.SendLEDataPacketResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSendLEDataPacketMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getStartAdvertisementMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_START_ADVERTISEMENT)))
          .addMethod(
            getMakeConnectionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.L2capProto.MakeConnectionRequest,
                pandora.L2capProto.MakeConnectionResponse>(
                  this, METHODID_MAKE_CONNECTION)))
          .addMethod(
            getSendLEDataPacketMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.L2capProto.SendLEDataPacketRequest,
                pandora.L2capProto.SendLEDataPacketResponse>(
                  this, METHODID_SEND_LEDATA_PACKET)))
          .build();
    }
  }

  /**
   */
  public static final class L2CAPStub extends io.grpc.stub.AbstractAsyncStub<L2CAPStub> {
    private L2CAPStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected L2CAPStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new L2CAPStub(channel, callOptions);
    }

    /**
     */
    public void startAdvertisement(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getStartAdvertisementMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void makeConnection(pandora.L2capProto.MakeConnectionRequest request,
        io.grpc.stub.StreamObserver<pandora.L2capProto.MakeConnectionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getMakeConnectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sendLEDataPacket(pandora.L2capProto.SendLEDataPacketRequest request,
        io.grpc.stub.StreamObserver<pandora.L2capProto.SendLEDataPacketResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSendLEDataPacketMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class L2CAPBlockingStub extends io.grpc.stub.AbstractBlockingStub<L2CAPBlockingStub> {
    private L2CAPBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected L2CAPBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new L2CAPBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.google.protobuf.Empty startAdvertisement(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getStartAdvertisementMethod(), getCallOptions(), request);
    }

    /**
     */
    public pandora.L2capProto.MakeConnectionResponse makeConnection(pandora.L2capProto.MakeConnectionRequest request) {
      return blockingUnaryCall(
          getChannel(), getMakeConnectionMethod(), getCallOptions(), request);
    }

    /**
     */
    public pandora.L2capProto.SendLEDataPacketResponse sendLEDataPacket(pandora.L2capProto.SendLEDataPacketRequest request) {
      return blockingUnaryCall(
          getChannel(), getSendLEDataPacketMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class L2CAPFutureStub extends io.grpc.stub.AbstractFutureStub<L2CAPFutureStub> {
    private L2CAPFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected L2CAPFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new L2CAPFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> startAdvertisement(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getStartAdvertisementMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.L2capProto.MakeConnectionResponse> makeConnection(
        pandora.L2capProto.MakeConnectionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getMakeConnectionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.L2capProto.SendLEDataPacketResponse> sendLEDataPacket(
        pandora.L2capProto.SendLEDataPacketRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSendLEDataPacketMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_START_ADVERTISEMENT = 0;
  private static final int METHODID_MAKE_CONNECTION = 1;
  private static final int METHODID_SEND_LEDATA_PACKET = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final L2CAPImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(L2CAPImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_START_ADVERTISEMENT:
          serviceImpl.startAdvertisement((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_MAKE_CONNECTION:
          serviceImpl.makeConnection((pandora.L2capProto.MakeConnectionRequest) request,
              (io.grpc.stub.StreamObserver<pandora.L2capProto.MakeConnectionResponse>) responseObserver);
          break;
        case METHODID_SEND_LEDATA_PACKET:
          serviceImpl.sendLEDataPacket((pandora.L2capProto.SendLEDataPacketRequest) request,
              (io.grpc.stub.StreamObserver<pandora.L2capProto.SendLEDataPacketResponse>) responseObserver);
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

  private static abstract class L2CAPBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    L2CAPBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return pandora.L2capProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("L2CAP");
    }
  }

  private static final class L2CAPFileDescriptorSupplier
      extends L2CAPBaseDescriptorSupplier {
    L2CAPFileDescriptorSupplier() {}
  }

  private static final class L2CAPMethodDescriptorSupplier
      extends L2CAPBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    L2CAPMethodDescriptorSupplier(String methodName) {
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
      synchronized (L2CAPGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new L2CAPFileDescriptorSupplier())
              .addMethod(getStartAdvertisementMethod())
              .addMethod(getMakeConnectionMethod())
              .addMethod(getSendLEDataPacketMethod())
              .build();
        }
      }
    }
    return result;
  }
}
