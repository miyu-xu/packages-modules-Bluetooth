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
 * Service to trigger A2DP (Advanced Audio Distribution Profile) procedures.
 * Requirements for the implementor:
 * - Streams must not be automatically opened, even if discovered.
 * - The `Host` service must be implemented
 * References:
 * - [A2DP] Bluetooth SIG, Specification of the Bluetooth System,
 *    Advanced Audio Distribution, Version 1.3 or Later
 * - [AVDTP] Bluetooth SIG, Specification of the Bluetooth System,
 *    Audio/Video Distribution Transport Protocol, Version 1.3 or Later
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: pandora/a2dp.proto")
public final class A2DPGrpc {

  private A2DPGrpc() {}

  public static final String SERVICE_NAME = "pandora.A2DP";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<pandora.A2dpProto.OpenSourceRequest,
      pandora.A2dpProto.OpenSourceResponse> getOpenSourceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "OpenSource",
      requestType = pandora.A2dpProto.OpenSourceRequest.class,
      responseType = pandora.A2dpProto.OpenSourceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.A2dpProto.OpenSourceRequest,
      pandora.A2dpProto.OpenSourceResponse> getOpenSourceMethod() {
    io.grpc.MethodDescriptor<pandora.A2dpProto.OpenSourceRequest, pandora.A2dpProto.OpenSourceResponse> getOpenSourceMethod;
    if ((getOpenSourceMethod = A2DPGrpc.getOpenSourceMethod) == null) {
      synchronized (A2DPGrpc.class) {
        if ((getOpenSourceMethod = A2DPGrpc.getOpenSourceMethod) == null) {
          A2DPGrpc.getOpenSourceMethod = getOpenSourceMethod =
              io.grpc.MethodDescriptor.<pandora.A2dpProto.OpenSourceRequest, pandora.A2dpProto.OpenSourceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "OpenSource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.OpenSourceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.OpenSourceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2DPMethodDescriptorSupplier("OpenSource"))
              .build();
        }
      }
    }
    return getOpenSourceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.A2dpProto.OpenSinkRequest,
      pandora.A2dpProto.OpenSinkResponse> getOpenSinkMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "OpenSink",
      requestType = pandora.A2dpProto.OpenSinkRequest.class,
      responseType = pandora.A2dpProto.OpenSinkResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.A2dpProto.OpenSinkRequest,
      pandora.A2dpProto.OpenSinkResponse> getOpenSinkMethod() {
    io.grpc.MethodDescriptor<pandora.A2dpProto.OpenSinkRequest, pandora.A2dpProto.OpenSinkResponse> getOpenSinkMethod;
    if ((getOpenSinkMethod = A2DPGrpc.getOpenSinkMethod) == null) {
      synchronized (A2DPGrpc.class) {
        if ((getOpenSinkMethod = A2DPGrpc.getOpenSinkMethod) == null) {
          A2DPGrpc.getOpenSinkMethod = getOpenSinkMethod =
              io.grpc.MethodDescriptor.<pandora.A2dpProto.OpenSinkRequest, pandora.A2dpProto.OpenSinkResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "OpenSink"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.OpenSinkRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.OpenSinkResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2DPMethodDescriptorSupplier("OpenSink"))
              .build();
        }
      }
    }
    return getOpenSinkMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.A2dpProto.WaitSourceRequest,
      pandora.A2dpProto.WaitSourceResponse> getWaitSourceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WaitSource",
      requestType = pandora.A2dpProto.WaitSourceRequest.class,
      responseType = pandora.A2dpProto.WaitSourceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.A2dpProto.WaitSourceRequest,
      pandora.A2dpProto.WaitSourceResponse> getWaitSourceMethod() {
    io.grpc.MethodDescriptor<pandora.A2dpProto.WaitSourceRequest, pandora.A2dpProto.WaitSourceResponse> getWaitSourceMethod;
    if ((getWaitSourceMethod = A2DPGrpc.getWaitSourceMethod) == null) {
      synchronized (A2DPGrpc.class) {
        if ((getWaitSourceMethod = A2DPGrpc.getWaitSourceMethod) == null) {
          A2DPGrpc.getWaitSourceMethod = getWaitSourceMethod =
              io.grpc.MethodDescriptor.<pandora.A2dpProto.WaitSourceRequest, pandora.A2dpProto.WaitSourceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WaitSource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.WaitSourceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.WaitSourceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2DPMethodDescriptorSupplier("WaitSource"))
              .build();
        }
      }
    }
    return getWaitSourceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.A2dpProto.WaitSinkRequest,
      pandora.A2dpProto.WaitSinkResponse> getWaitSinkMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WaitSink",
      requestType = pandora.A2dpProto.WaitSinkRequest.class,
      responseType = pandora.A2dpProto.WaitSinkResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.A2dpProto.WaitSinkRequest,
      pandora.A2dpProto.WaitSinkResponse> getWaitSinkMethod() {
    io.grpc.MethodDescriptor<pandora.A2dpProto.WaitSinkRequest, pandora.A2dpProto.WaitSinkResponse> getWaitSinkMethod;
    if ((getWaitSinkMethod = A2DPGrpc.getWaitSinkMethod) == null) {
      synchronized (A2DPGrpc.class) {
        if ((getWaitSinkMethod = A2DPGrpc.getWaitSinkMethod) == null) {
          A2DPGrpc.getWaitSinkMethod = getWaitSinkMethod =
              io.grpc.MethodDescriptor.<pandora.A2dpProto.WaitSinkRequest, pandora.A2dpProto.WaitSinkResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WaitSink"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.WaitSinkRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.WaitSinkResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2DPMethodDescriptorSupplier("WaitSink"))
              .build();
        }
      }
    }
    return getWaitSinkMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.A2dpProto.IsSuspendedRequest,
      pandora.A2dpProto.IsSuspendedResponse> getIsSuspendedMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "IsSuspended",
      requestType = pandora.A2dpProto.IsSuspendedRequest.class,
      responseType = pandora.A2dpProto.IsSuspendedResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.A2dpProto.IsSuspendedRequest,
      pandora.A2dpProto.IsSuspendedResponse> getIsSuspendedMethod() {
    io.grpc.MethodDescriptor<pandora.A2dpProto.IsSuspendedRequest, pandora.A2dpProto.IsSuspendedResponse> getIsSuspendedMethod;
    if ((getIsSuspendedMethod = A2DPGrpc.getIsSuspendedMethod) == null) {
      synchronized (A2DPGrpc.class) {
        if ((getIsSuspendedMethod = A2DPGrpc.getIsSuspendedMethod) == null) {
          A2DPGrpc.getIsSuspendedMethod = getIsSuspendedMethod =
              io.grpc.MethodDescriptor.<pandora.A2dpProto.IsSuspendedRequest, pandora.A2dpProto.IsSuspendedResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "IsSuspended"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.IsSuspendedRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.IsSuspendedResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2DPMethodDescriptorSupplier("IsSuspended"))
              .build();
        }
      }
    }
    return getIsSuspendedMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.A2dpProto.StartRequest,
      pandora.A2dpProto.StartResponse> getStartMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Start",
      requestType = pandora.A2dpProto.StartRequest.class,
      responseType = pandora.A2dpProto.StartResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.A2dpProto.StartRequest,
      pandora.A2dpProto.StartResponse> getStartMethod() {
    io.grpc.MethodDescriptor<pandora.A2dpProto.StartRequest, pandora.A2dpProto.StartResponse> getStartMethod;
    if ((getStartMethod = A2DPGrpc.getStartMethod) == null) {
      synchronized (A2DPGrpc.class) {
        if ((getStartMethod = A2DPGrpc.getStartMethod) == null) {
          A2DPGrpc.getStartMethod = getStartMethod =
              io.grpc.MethodDescriptor.<pandora.A2dpProto.StartRequest, pandora.A2dpProto.StartResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Start"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.StartRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.StartResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2DPMethodDescriptorSupplier("Start"))
              .build();
        }
      }
    }
    return getStartMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.A2dpProto.SuspendRequest,
      pandora.A2dpProto.SuspendResponse> getSuspendMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Suspend",
      requestType = pandora.A2dpProto.SuspendRequest.class,
      responseType = pandora.A2dpProto.SuspendResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.A2dpProto.SuspendRequest,
      pandora.A2dpProto.SuspendResponse> getSuspendMethod() {
    io.grpc.MethodDescriptor<pandora.A2dpProto.SuspendRequest, pandora.A2dpProto.SuspendResponse> getSuspendMethod;
    if ((getSuspendMethod = A2DPGrpc.getSuspendMethod) == null) {
      synchronized (A2DPGrpc.class) {
        if ((getSuspendMethod = A2DPGrpc.getSuspendMethod) == null) {
          A2DPGrpc.getSuspendMethod = getSuspendMethod =
              io.grpc.MethodDescriptor.<pandora.A2dpProto.SuspendRequest, pandora.A2dpProto.SuspendResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Suspend"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.SuspendRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.SuspendResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2DPMethodDescriptorSupplier("Suspend"))
              .build();
        }
      }
    }
    return getSuspendMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.A2dpProto.CloseRequest,
      pandora.A2dpProto.CloseResponse> getCloseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Close",
      requestType = pandora.A2dpProto.CloseRequest.class,
      responseType = pandora.A2dpProto.CloseResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.A2dpProto.CloseRequest,
      pandora.A2dpProto.CloseResponse> getCloseMethod() {
    io.grpc.MethodDescriptor<pandora.A2dpProto.CloseRequest, pandora.A2dpProto.CloseResponse> getCloseMethod;
    if ((getCloseMethod = A2DPGrpc.getCloseMethod) == null) {
      synchronized (A2DPGrpc.class) {
        if ((getCloseMethod = A2DPGrpc.getCloseMethod) == null) {
          A2DPGrpc.getCloseMethod = getCloseMethod =
              io.grpc.MethodDescriptor.<pandora.A2dpProto.CloseRequest, pandora.A2dpProto.CloseResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Close"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.CloseRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.CloseResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2DPMethodDescriptorSupplier("Close"))
              .build();
        }
      }
    }
    return getCloseMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.A2dpProto.GetAudioEncodingRequest,
      pandora.A2dpProto.GetAudioEncodingResponse> getGetAudioEncodingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAudioEncoding",
      requestType = pandora.A2dpProto.GetAudioEncodingRequest.class,
      responseType = pandora.A2dpProto.GetAudioEncodingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.A2dpProto.GetAudioEncodingRequest,
      pandora.A2dpProto.GetAudioEncodingResponse> getGetAudioEncodingMethod() {
    io.grpc.MethodDescriptor<pandora.A2dpProto.GetAudioEncodingRequest, pandora.A2dpProto.GetAudioEncodingResponse> getGetAudioEncodingMethod;
    if ((getGetAudioEncodingMethod = A2DPGrpc.getGetAudioEncodingMethod) == null) {
      synchronized (A2DPGrpc.class) {
        if ((getGetAudioEncodingMethod = A2DPGrpc.getGetAudioEncodingMethod) == null) {
          A2DPGrpc.getGetAudioEncodingMethod = getGetAudioEncodingMethod =
              io.grpc.MethodDescriptor.<pandora.A2dpProto.GetAudioEncodingRequest, pandora.A2dpProto.GetAudioEncodingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAudioEncoding"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.GetAudioEncodingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.GetAudioEncodingResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2DPMethodDescriptorSupplier("GetAudioEncoding"))
              .build();
        }
      }
    }
    return getGetAudioEncodingMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.A2dpProto.PlaybackAudioRequest,
      pandora.A2dpProto.PlaybackAudioResponse> getPlaybackAudioMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PlaybackAudio",
      requestType = pandora.A2dpProto.PlaybackAudioRequest.class,
      responseType = pandora.A2dpProto.PlaybackAudioResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<pandora.A2dpProto.PlaybackAudioRequest,
      pandora.A2dpProto.PlaybackAudioResponse> getPlaybackAudioMethod() {
    io.grpc.MethodDescriptor<pandora.A2dpProto.PlaybackAudioRequest, pandora.A2dpProto.PlaybackAudioResponse> getPlaybackAudioMethod;
    if ((getPlaybackAudioMethod = A2DPGrpc.getPlaybackAudioMethod) == null) {
      synchronized (A2DPGrpc.class) {
        if ((getPlaybackAudioMethod = A2DPGrpc.getPlaybackAudioMethod) == null) {
          A2DPGrpc.getPlaybackAudioMethod = getPlaybackAudioMethod =
              io.grpc.MethodDescriptor.<pandora.A2dpProto.PlaybackAudioRequest, pandora.A2dpProto.PlaybackAudioResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PlaybackAudio"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.PlaybackAudioRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.PlaybackAudioResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2DPMethodDescriptorSupplier("PlaybackAudio"))
              .build();
        }
      }
    }
    return getPlaybackAudioMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.A2dpProto.CaptureAudioRequest,
      pandora.A2dpProto.CaptureAudioResponse> getCaptureAudioMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CaptureAudio",
      requestType = pandora.A2dpProto.CaptureAudioRequest.class,
      responseType = pandora.A2dpProto.CaptureAudioResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<pandora.A2dpProto.CaptureAudioRequest,
      pandora.A2dpProto.CaptureAudioResponse> getCaptureAudioMethod() {
    io.grpc.MethodDescriptor<pandora.A2dpProto.CaptureAudioRequest, pandora.A2dpProto.CaptureAudioResponse> getCaptureAudioMethod;
    if ((getCaptureAudioMethod = A2DPGrpc.getCaptureAudioMethod) == null) {
      synchronized (A2DPGrpc.class) {
        if ((getCaptureAudioMethod = A2DPGrpc.getCaptureAudioMethod) == null) {
          A2DPGrpc.getCaptureAudioMethod = getCaptureAudioMethod =
              io.grpc.MethodDescriptor.<pandora.A2dpProto.CaptureAudioRequest, pandora.A2dpProto.CaptureAudioResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CaptureAudio"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.CaptureAudioRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.A2dpProto.CaptureAudioResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2DPMethodDescriptorSupplier("CaptureAudio"))
              .build();
        }
      }
    }
    return getCaptureAudioMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static A2DPStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<A2DPStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<A2DPStub>() {
        @java.lang.Override
        public A2DPStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new A2DPStub(channel, callOptions);
        }
      };
    return A2DPStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static A2DPBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<A2DPBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<A2DPBlockingStub>() {
        @java.lang.Override
        public A2DPBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new A2DPBlockingStub(channel, callOptions);
        }
      };
    return A2DPBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static A2DPFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<A2DPFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<A2DPFutureStub>() {
        @java.lang.Override
        public A2DPFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new A2DPFutureStub(channel, callOptions);
        }
      };
    return A2DPFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Service to trigger A2DP (Advanced Audio Distribution Profile) procedures.
   * Requirements for the implementor:
   * - Streams must not be automatically opened, even if discovered.
   * - The `Host` service must be implemented
   * References:
   * - [A2DP] Bluetooth SIG, Specification of the Bluetooth System,
   *    Advanced Audio Distribution, Version 1.3 or Later
   * - [AVDTP] Bluetooth SIG, Specification of the Bluetooth System,
   *    Audio/Video Distribution Transport Protocol, Version 1.3 or Later
   * </pre>
   */
  public static abstract class A2DPImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Open a stream from a local **Source** endpoint to a remote **Sink**
     * endpoint.
     * The returned source should be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * A cancellation of this call must result in aborting the current
     * AVDTP procedure (see [AVDTP] 9.9).
     * </pre>
     */
    public void openSource(pandora.A2dpProto.OpenSourceRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.OpenSourceResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getOpenSourceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Open a stream from a local **Sink** endpoint to a remote **Source**
     * endpoint.
     * The returned sink must be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * A cancellation of this call must result in aborting the current
     * AVDTP procedure (see [AVDTP] 9.9).
     * </pre>
     */
    public void openSink(pandora.A2dpProto.OpenSinkRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.OpenSinkResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getOpenSinkMethod(), responseObserver);
    }

    /**
     * <pre>
     * Wait for a stream from a local **Source** endpoint to
     * a remote **Sink** endpoint to open.
     * The returned source should be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * If the peer has opened a source prior to this call, the server will
     * return it. The server must return the same source only once.
     * </pre>
     */
    public void waitSource(pandora.A2dpProto.WaitSourceRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.WaitSourceResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getWaitSourceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Wait for a stream from a local **Sink** endpoint to
     * a remote **Source** endpoint to open.
     * The returned sink should be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * If the peer has opened a sink prior to this call, the server will
     * return it. The server must return the same sink only once.
     * </pre>
     */
    public void waitSink(pandora.A2dpProto.WaitSinkRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.WaitSinkResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getWaitSinkMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get if the stream is suspended
     * </pre>
     */
    public void isSuspended(pandora.A2dpProto.IsSuspendedRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.IsSuspendedResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getIsSuspendedMethod(), responseObserver);
    }

    /**
     * <pre>
     * Start a suspended stream.
     * </pre>
     */
    public void start(pandora.A2dpProto.StartRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.StartResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getStartMethod(), responseObserver);
    }

    /**
     * <pre>
     * Suspend a started stream.
     * </pre>
     */
    public void suspend(pandora.A2dpProto.SuspendRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.SuspendResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSuspendMethod(), responseObserver);
    }

    /**
     * <pre>
     * Close a stream, the source or sink tokens must not be reused afterwards.
     * </pre>
     */
    public void close(pandora.A2dpProto.CloseRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.CloseResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCloseMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the `AudioEncoding` value of a stream
     * </pre>
     */
    public void getAudioEncoding(pandora.A2dpProto.GetAudioEncodingRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.GetAudioEncodingResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAudioEncodingMethod(), responseObserver);
    }

    /**
     * <pre>
     * Playback audio by a `Source`
     * </pre>
     */
    public io.grpc.stub.StreamObserver<pandora.A2dpProto.PlaybackAudioRequest> playbackAudio(
        io.grpc.stub.StreamObserver<pandora.A2dpProto.PlaybackAudioResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(getPlaybackAudioMethod(), responseObserver);
    }

    /**
     * <pre>
     * Capture audio from a `Sink`
     * </pre>
     */
    public void captureAudio(pandora.A2dpProto.CaptureAudioRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.CaptureAudioResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCaptureAudioMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getOpenSourceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.A2dpProto.OpenSourceRequest,
                pandora.A2dpProto.OpenSourceResponse>(
                  this, METHODID_OPEN_SOURCE)))
          .addMethod(
            getOpenSinkMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.A2dpProto.OpenSinkRequest,
                pandora.A2dpProto.OpenSinkResponse>(
                  this, METHODID_OPEN_SINK)))
          .addMethod(
            getWaitSourceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.A2dpProto.WaitSourceRequest,
                pandora.A2dpProto.WaitSourceResponse>(
                  this, METHODID_WAIT_SOURCE)))
          .addMethod(
            getWaitSinkMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.A2dpProto.WaitSinkRequest,
                pandora.A2dpProto.WaitSinkResponse>(
                  this, METHODID_WAIT_SINK)))
          .addMethod(
            getIsSuspendedMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.A2dpProto.IsSuspendedRequest,
                pandora.A2dpProto.IsSuspendedResponse>(
                  this, METHODID_IS_SUSPENDED)))
          .addMethod(
            getStartMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.A2dpProto.StartRequest,
                pandora.A2dpProto.StartResponse>(
                  this, METHODID_START)))
          .addMethod(
            getSuspendMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.A2dpProto.SuspendRequest,
                pandora.A2dpProto.SuspendResponse>(
                  this, METHODID_SUSPEND)))
          .addMethod(
            getCloseMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.A2dpProto.CloseRequest,
                pandora.A2dpProto.CloseResponse>(
                  this, METHODID_CLOSE)))
          .addMethod(
            getGetAudioEncodingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.A2dpProto.GetAudioEncodingRequest,
                pandora.A2dpProto.GetAudioEncodingResponse>(
                  this, METHODID_GET_AUDIO_ENCODING)))
          .addMethod(
            getPlaybackAudioMethod(),
            asyncClientStreamingCall(
              new MethodHandlers<
                pandora.A2dpProto.PlaybackAudioRequest,
                pandora.A2dpProto.PlaybackAudioResponse>(
                  this, METHODID_PLAYBACK_AUDIO)))
          .addMethod(
            getCaptureAudioMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                pandora.A2dpProto.CaptureAudioRequest,
                pandora.A2dpProto.CaptureAudioResponse>(
                  this, METHODID_CAPTURE_AUDIO)))
          .build();
    }
  }

  /**
   * <pre>
   * Service to trigger A2DP (Advanced Audio Distribution Profile) procedures.
   * Requirements for the implementor:
   * - Streams must not be automatically opened, even if discovered.
   * - The `Host` service must be implemented
   * References:
   * - [A2DP] Bluetooth SIG, Specification of the Bluetooth System,
   *    Advanced Audio Distribution, Version 1.3 or Later
   * - [AVDTP] Bluetooth SIG, Specification of the Bluetooth System,
   *    Audio/Video Distribution Transport Protocol, Version 1.3 or Later
   * </pre>
   */
  public static final class A2DPStub extends io.grpc.stub.AbstractAsyncStub<A2DPStub> {
    private A2DPStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected A2DPStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new A2DPStub(channel, callOptions);
    }

    /**
     * <pre>
     * Open a stream from a local **Source** endpoint to a remote **Sink**
     * endpoint.
     * The returned source should be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * A cancellation of this call must result in aborting the current
     * AVDTP procedure (see [AVDTP] 9.9).
     * </pre>
     */
    public void openSource(pandora.A2dpProto.OpenSourceRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.OpenSourceResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getOpenSourceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Open a stream from a local **Sink** endpoint to a remote **Source**
     * endpoint.
     * The returned sink must be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * A cancellation of this call must result in aborting the current
     * AVDTP procedure (see [AVDTP] 9.9).
     * </pre>
     */
    public void openSink(pandora.A2dpProto.OpenSinkRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.OpenSinkResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getOpenSinkMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Wait for a stream from a local **Source** endpoint to
     * a remote **Sink** endpoint to open.
     * The returned source should be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * If the peer has opened a source prior to this call, the server will
     * return it. The server must return the same source only once.
     * </pre>
     */
    public void waitSource(pandora.A2dpProto.WaitSourceRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.WaitSourceResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWaitSourceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Wait for a stream from a local **Sink** endpoint to
     * a remote **Source** endpoint to open.
     * The returned sink should be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * If the peer has opened a sink prior to this call, the server will
     * return it. The server must return the same sink only once.
     * </pre>
     */
    public void waitSink(pandora.A2dpProto.WaitSinkRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.WaitSinkResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWaitSinkMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get if the stream is suspended
     * </pre>
     */
    public void isSuspended(pandora.A2dpProto.IsSuspendedRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.IsSuspendedResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getIsSuspendedMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Start a suspended stream.
     * </pre>
     */
    public void start(pandora.A2dpProto.StartRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.StartResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getStartMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Suspend a started stream.
     * </pre>
     */
    public void suspend(pandora.A2dpProto.SuspendRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.SuspendResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSuspendMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Close a stream, the source or sink tokens must not be reused afterwards.
     * </pre>
     */
    public void close(pandora.A2dpProto.CloseRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.CloseResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCloseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the `AudioEncoding` value of a stream
     * </pre>
     */
    public void getAudioEncoding(pandora.A2dpProto.GetAudioEncodingRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.GetAudioEncodingResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAudioEncodingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Playback audio by a `Source`
     * </pre>
     */
    public io.grpc.stub.StreamObserver<pandora.A2dpProto.PlaybackAudioRequest> playbackAudio(
        io.grpc.stub.StreamObserver<pandora.A2dpProto.PlaybackAudioResponse> responseObserver) {
      return asyncClientStreamingCall(
          getChannel().newCall(getPlaybackAudioMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * Capture audio from a `Sink`
     * </pre>
     */
    public void captureAudio(pandora.A2dpProto.CaptureAudioRequest request,
        io.grpc.stub.StreamObserver<pandora.A2dpProto.CaptureAudioResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getCaptureAudioMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Service to trigger A2DP (Advanced Audio Distribution Profile) procedures.
   * Requirements for the implementor:
   * - Streams must not be automatically opened, even if discovered.
   * - The `Host` service must be implemented
   * References:
   * - [A2DP] Bluetooth SIG, Specification of the Bluetooth System,
   *    Advanced Audio Distribution, Version 1.3 or Later
   * - [AVDTP] Bluetooth SIG, Specification of the Bluetooth System,
   *    Audio/Video Distribution Transport Protocol, Version 1.3 or Later
   * </pre>
   */
  public static final class A2DPBlockingStub extends io.grpc.stub.AbstractBlockingStub<A2DPBlockingStub> {
    private A2DPBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected A2DPBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new A2DPBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Open a stream from a local **Source** endpoint to a remote **Sink**
     * endpoint.
     * The returned source should be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * A cancellation of this call must result in aborting the current
     * AVDTP procedure (see [AVDTP] 9.9).
     * </pre>
     */
    public pandora.A2dpProto.OpenSourceResponse openSource(pandora.A2dpProto.OpenSourceRequest request) {
      return blockingUnaryCall(
          getChannel(), getOpenSourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Open a stream from a local **Sink** endpoint to a remote **Source**
     * endpoint.
     * The returned sink must be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * A cancellation of this call must result in aborting the current
     * AVDTP procedure (see [AVDTP] 9.9).
     * </pre>
     */
    public pandora.A2dpProto.OpenSinkResponse openSink(pandora.A2dpProto.OpenSinkRequest request) {
      return blockingUnaryCall(
          getChannel(), getOpenSinkMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Wait for a stream from a local **Source** endpoint to
     * a remote **Sink** endpoint to open.
     * The returned source should be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * If the peer has opened a source prior to this call, the server will
     * return it. The server must return the same source only once.
     * </pre>
     */
    public pandora.A2dpProto.WaitSourceResponse waitSource(pandora.A2dpProto.WaitSourceRequest request) {
      return blockingUnaryCall(
          getChannel(), getWaitSourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Wait for a stream from a local **Sink** endpoint to
     * a remote **Source** endpoint to open.
     * The returned sink should be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * If the peer has opened a sink prior to this call, the server will
     * return it. The server must return the same sink only once.
     * </pre>
     */
    public pandora.A2dpProto.WaitSinkResponse waitSink(pandora.A2dpProto.WaitSinkRequest request) {
      return blockingUnaryCall(
          getChannel(), getWaitSinkMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get if the stream is suspended
     * </pre>
     */
    public pandora.A2dpProto.IsSuspendedResponse isSuspended(pandora.A2dpProto.IsSuspendedRequest request) {
      return blockingUnaryCall(
          getChannel(), getIsSuspendedMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Start a suspended stream.
     * </pre>
     */
    public pandora.A2dpProto.StartResponse start(pandora.A2dpProto.StartRequest request) {
      return blockingUnaryCall(
          getChannel(), getStartMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Suspend a started stream.
     * </pre>
     */
    public pandora.A2dpProto.SuspendResponse suspend(pandora.A2dpProto.SuspendRequest request) {
      return blockingUnaryCall(
          getChannel(), getSuspendMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Close a stream, the source or sink tokens must not be reused afterwards.
     * </pre>
     */
    public pandora.A2dpProto.CloseResponse close(pandora.A2dpProto.CloseRequest request) {
      return blockingUnaryCall(
          getChannel(), getCloseMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the `AudioEncoding` value of a stream
     * </pre>
     */
    public pandora.A2dpProto.GetAudioEncodingResponse getAudioEncoding(pandora.A2dpProto.GetAudioEncodingRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetAudioEncodingMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Capture audio from a `Sink`
     * </pre>
     */
    public java.util.Iterator<pandora.A2dpProto.CaptureAudioResponse> captureAudio(
        pandora.A2dpProto.CaptureAudioRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getCaptureAudioMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Service to trigger A2DP (Advanced Audio Distribution Profile) procedures.
   * Requirements for the implementor:
   * - Streams must not be automatically opened, even if discovered.
   * - The `Host` service must be implemented
   * References:
   * - [A2DP] Bluetooth SIG, Specification of the Bluetooth System,
   *    Advanced Audio Distribution, Version 1.3 or Later
   * - [AVDTP] Bluetooth SIG, Specification of the Bluetooth System,
   *    Audio/Video Distribution Transport Protocol, Version 1.3 or Later
   * </pre>
   */
  public static final class A2DPFutureStub extends io.grpc.stub.AbstractFutureStub<A2DPFutureStub> {
    private A2DPFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected A2DPFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new A2DPFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Open a stream from a local **Source** endpoint to a remote **Sink**
     * endpoint.
     * The returned source should be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * A cancellation of this call must result in aborting the current
     * AVDTP procedure (see [AVDTP] 9.9).
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.A2dpProto.OpenSourceResponse> openSource(
        pandora.A2dpProto.OpenSourceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getOpenSourceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Open a stream from a local **Sink** endpoint to a remote **Source**
     * endpoint.
     * The returned sink must be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * A cancellation of this call must result in aborting the current
     * AVDTP procedure (see [AVDTP] 9.9).
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.A2dpProto.OpenSinkResponse> openSink(
        pandora.A2dpProto.OpenSinkRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getOpenSinkMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Wait for a stream from a local **Source** endpoint to
     * a remote **Sink** endpoint to open.
     * The returned source should be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * If the peer has opened a source prior to this call, the server will
     * return it. The server must return the same source only once.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.A2dpProto.WaitSourceResponse> waitSource(
        pandora.A2dpProto.WaitSourceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getWaitSourceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Wait for a stream from a local **Sink** endpoint to
     * a remote **Source** endpoint to open.
     * The returned sink should be in the AVDTP_OPEN state (see [AVDTP] 9.1).
     * The rpc must block until the stream has reached this state.
     * If the peer has opened a sink prior to this call, the server will
     * return it. The server must return the same sink only once.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.A2dpProto.WaitSinkResponse> waitSink(
        pandora.A2dpProto.WaitSinkRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getWaitSinkMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get if the stream is suspended
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.A2dpProto.IsSuspendedResponse> isSuspended(
        pandora.A2dpProto.IsSuspendedRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getIsSuspendedMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Start a suspended stream.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.A2dpProto.StartResponse> start(
        pandora.A2dpProto.StartRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getStartMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Suspend a started stream.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.A2dpProto.SuspendResponse> suspend(
        pandora.A2dpProto.SuspendRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSuspendMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Close a stream, the source or sink tokens must not be reused afterwards.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.A2dpProto.CloseResponse> close(
        pandora.A2dpProto.CloseRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCloseMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the `AudioEncoding` value of a stream
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.A2dpProto.GetAudioEncodingResponse> getAudioEncoding(
        pandora.A2dpProto.GetAudioEncodingRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAudioEncodingMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_OPEN_SOURCE = 0;
  private static final int METHODID_OPEN_SINK = 1;
  private static final int METHODID_WAIT_SOURCE = 2;
  private static final int METHODID_WAIT_SINK = 3;
  private static final int METHODID_IS_SUSPENDED = 4;
  private static final int METHODID_START = 5;
  private static final int METHODID_SUSPEND = 6;
  private static final int METHODID_CLOSE = 7;
  private static final int METHODID_GET_AUDIO_ENCODING = 8;
  private static final int METHODID_CAPTURE_AUDIO = 9;
  private static final int METHODID_PLAYBACK_AUDIO = 10;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final A2DPImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(A2DPImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_OPEN_SOURCE:
          serviceImpl.openSource((pandora.A2dpProto.OpenSourceRequest) request,
              (io.grpc.stub.StreamObserver<pandora.A2dpProto.OpenSourceResponse>) responseObserver);
          break;
        case METHODID_OPEN_SINK:
          serviceImpl.openSink((pandora.A2dpProto.OpenSinkRequest) request,
              (io.grpc.stub.StreamObserver<pandora.A2dpProto.OpenSinkResponse>) responseObserver);
          break;
        case METHODID_WAIT_SOURCE:
          serviceImpl.waitSource((pandora.A2dpProto.WaitSourceRequest) request,
              (io.grpc.stub.StreamObserver<pandora.A2dpProto.WaitSourceResponse>) responseObserver);
          break;
        case METHODID_WAIT_SINK:
          serviceImpl.waitSink((pandora.A2dpProto.WaitSinkRequest) request,
              (io.grpc.stub.StreamObserver<pandora.A2dpProto.WaitSinkResponse>) responseObserver);
          break;
        case METHODID_IS_SUSPENDED:
          serviceImpl.isSuspended((pandora.A2dpProto.IsSuspendedRequest) request,
              (io.grpc.stub.StreamObserver<pandora.A2dpProto.IsSuspendedResponse>) responseObserver);
          break;
        case METHODID_START:
          serviceImpl.start((pandora.A2dpProto.StartRequest) request,
              (io.grpc.stub.StreamObserver<pandora.A2dpProto.StartResponse>) responseObserver);
          break;
        case METHODID_SUSPEND:
          serviceImpl.suspend((pandora.A2dpProto.SuspendRequest) request,
              (io.grpc.stub.StreamObserver<pandora.A2dpProto.SuspendResponse>) responseObserver);
          break;
        case METHODID_CLOSE:
          serviceImpl.close((pandora.A2dpProto.CloseRequest) request,
              (io.grpc.stub.StreamObserver<pandora.A2dpProto.CloseResponse>) responseObserver);
          break;
        case METHODID_GET_AUDIO_ENCODING:
          serviceImpl.getAudioEncoding((pandora.A2dpProto.GetAudioEncodingRequest) request,
              (io.grpc.stub.StreamObserver<pandora.A2dpProto.GetAudioEncodingResponse>) responseObserver);
          break;
        case METHODID_CAPTURE_AUDIO:
          serviceImpl.captureAudio((pandora.A2dpProto.CaptureAudioRequest) request,
              (io.grpc.stub.StreamObserver<pandora.A2dpProto.CaptureAudioResponse>) responseObserver);
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
        case METHODID_PLAYBACK_AUDIO:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.playbackAudio(
              (io.grpc.stub.StreamObserver<pandora.A2dpProto.PlaybackAudioResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class A2DPBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    A2DPBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return pandora.A2dpProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("A2DP");
    }
  }

  private static final class A2DPFileDescriptorSupplier
      extends A2DPBaseDescriptorSupplier {
    A2DPFileDescriptorSupplier() {}
  }

  private static final class A2DPMethodDescriptorSupplier
      extends A2DPBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    A2DPMethodDescriptorSupplier(String methodName) {
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
      synchronized (A2DPGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new A2DPFileDescriptorSupplier())
              .addMethod(getOpenSourceMethod())
              .addMethod(getOpenSinkMethod())
              .addMethod(getWaitSourceMethod())
              .addMethod(getWaitSinkMethod())
              .addMethod(getIsSuspendedMethod())
              .addMethod(getStartMethod())
              .addMethod(getSuspendMethod())
              .addMethod(getCloseMethod())
              .addMethod(getGetAudioEncodingMethod())
              .addMethod(getPlaybackAudioMethod())
              .addMethod(getCaptureAudioMethod())
              .build();
        }
      }
    }
    return result;
  }
}
