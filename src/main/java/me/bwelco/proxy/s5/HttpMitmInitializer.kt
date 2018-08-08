package me.bwelco.proxy.s5

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.CustomNioSocketChannel
import java.net.Socket

class HttpMitmInitializer(val promise: Promise<Channel>,
                          val connectListener: (Socket) -> Unit) : ChannelInboundHandlerAdapter() {

    override fun channelRegistered(ctx: ChannelHandlerContext) {
        connectListener((ctx.channel() as CustomNioSocketChannel).rawSocket)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.pipeline().remove(this)
        promise.setSuccess(ctx.channel())
    }

}