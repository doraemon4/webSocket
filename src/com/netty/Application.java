package com.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 程序的入口，启动应用
 * @author Administrator
 *
 */
public class Application {
	public static void main(String[] args) {
		EventLoopGroup boosGroup=new NioEventLoopGroup();
		EventLoopGroup workGroup=new NioEventLoopGroup();
		try{
			ServerBootstrap bootstrap=new ServerBootstrap();
			bootstrap.group(boosGroup, workGroup);
			bootstrap.channel(NioServerSocketChannel.class);
			bootstrap.childHandler(new MyWebSocketChannelInitializer());
			System.out.println("服务器开启等待客户端连接");
			Channel channel=bootstrap.bind(8888).sync().channel();
			channel.closeFuture().sync();
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			boosGroup.shutdownGracefully();
			workGroup.shutdownGracefully();
		}
	}
}
