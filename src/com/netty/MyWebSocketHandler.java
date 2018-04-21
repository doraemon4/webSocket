package com.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

import java.util.Date;

public class MyWebSocketHandler extends SimpleChannelInboundHandler<Object>{
	private WebSocketServerHandshaker handshaker;
	private static final String WEB_SOCKET_URL="ws://localhost:8888";
	//客户端与服务端建立连接的时候调用
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		NettyConfig.group.add(ctx.channel());
		System.out.println("客户端与服务端建立连接..........");
	}

	//客户端与服务端断开连接的时候调用
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		NettyConfig.group.remove(ctx.channel());
		System.out.println("客户端与服务端断开连接..........");
	}

	//服务端接收客户端发送过来的数据结束调用
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	//工程出现异常的时候调用
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	//服务端处理客户端发来的请求
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		//处理客户端向服务端发起的http握手请求
		if(msg instanceof FullHttpRequest){
			handHttpRequest(ctx,(FullHttpRequest) msg);
		}else if(msg instanceof WebSocketFrame){  //处理websocket业务
			handWebSocketFrame(ctx,(WebSocketFrame) msg);
		}
	}
	
	/**
	 * 处理客户端向服务端发起的http握手请求
	 * @param ctx
	 * @param request
	 */
	private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
		if(!req.decoderResult().isSuccess()||!"websocket".equals(req.headers().get("Upgrade"))){
			DefaultFullHttpResponse res = new DefaultFullHttpResponse(
					HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
			senHttpResponse(ctx,req,res);
			return;
		}
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
				WEB_SOCKET_URL, null, false);
		handshaker = wsFactory.newHandshaker(req);
		if(handshaker==null){
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
		}else{
			handshaker.handshake(ctx.channel(), req);
		}
	}
	
	/**
	 * 处理客户端和服务端的websocket的业务
	 * @param ctx
	 * @param frame
	 */
	private void handWebSocketFrame(ChannelHandlerContext ctx,WebSocketFrame frame){
		//关闭websocket的指令
		if(frame instanceof CloseWebSocketFrame){
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			return;
		}else if(frame instanceof PingWebSocketFrame){//ping消息
			ctx.write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}else if(!(frame instanceof TextWebSocketFrame)){ //二进制消息
			System.out.println("目前我们不支持二级制消息");
			throw new RuntimeException("【"+this.getClass().getName()+"】不支持消息");
		}
		String request=((TextWebSocketFrame)frame).text();
		System.out.println("服务端收到的客户端的消息======>>>>>>>"+request);
		
		StringBuffer buffer=new StringBuffer();
		buffer.append(new Date().toString());
		buffer.append(" ");
		buffer.append(ctx.channel().id()).append(" ");
		buffer.append(request);
		TextWebSocketFrame tws = new TextWebSocketFrame(buffer.toString());
		
		//群发消息
		NettyConfig.group.writeAndFlush(tws);
		
	}
	/**
	 * 服务端响应信息
	 * @param ctx
	 * @param req
	 * @param res
	 */
	private void senHttpResponse(ChannelHandlerContext ctx,
			FullHttpRequest req, DefaultFullHttpResponse res) {
		if(res.status().code()!=200){
			ByteBuf buf=Unpooled.copiedBuffer(res.status().toString(),CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
		}
		//服务端向客户端发送数据
		ChannelFuture future=ctx.channel().writeAndFlush(res);
		if(res.status().code()!=200){
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}
}
