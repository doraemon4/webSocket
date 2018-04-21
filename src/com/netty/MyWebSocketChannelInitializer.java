package com.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 初始化连接的各个组件
 * @author Administrator
 *
 */
public class MyWebSocketChannelInitializer extends ChannelInitializer<SocketChannel>{

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast("http-codec",new HttpServerCodec());
		ch.pipeline().addLast("aggregator",new HttpObjectAggregator(65536));
		ch.pipeline().addLast("http-chunked",new ChunkedWriteHandler());
		ch.pipeline().addLast("handler",new MyWebSocketHandler());
	}

}
