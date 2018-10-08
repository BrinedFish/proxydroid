package me.bwelco.proxy.downstream

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.socksx.SocksMessage
import io.netty.handler.codec.socksx.SocksVersion
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest
import io.netty.handler.codec.socksx.v4.Socks4CommandType
import io.netty.handler.codec.socksx.v5.*
import me.bwelco.proxy.proxy.UpstreamMatchHandler
import me.bwelco.proxy.util.closeOnFlush

@ChannelHandler.Sharable
class SocksServerHandler(val upstreamMatchHandler: UpstreamMatchHandler) : SimpleChannelInboundHandler<SocksMessage>() {

    override fun channelRead0(ctx: ChannelHandlerContext, socksRequest: SocksMessage) {

        when (socksRequest.version()) {
            SocksVersion.SOCKS4a -> {
                val socksV4CmdRequest = socksRequest as Socks4CommandRequest
                if (socksV4CmdRequest.type() === Socks4CommandType.CONNECT) {
                    ctx.pipeline().addLast(upstreamMatchHandler)
                    ctx.pipeline().remove(this)
                    ctx.fireChannelRead(socksRequest)
                } else {
                    ctx.close()
                }
            }
            SocksVersion.SOCKS5 -> {
                if (socksRequest is Socks5InitialRequest) {
                    // auth support example
                    //ctx.pipeline().addFirst(new Socks5PasswordAuthRequestDecoder());
                    //ctx.write(new DefaultSocks5AuthMethodResponse(Socks5AuthMethod.PASSWORD));
                    ctx.pipeline().addFirst(Socks5CommandRequestDecoder())
                    ctx.write(DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH))
                } else if (socksRequest is Socks5PasswordAuthRequest) {
                    ctx.pipeline().addFirst(Socks5CommandRequestDecoder())
                    ctx.write(DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS))
                } else if (socksRequest is Socks5CommandRequest) {
                    if (socksRequest.type() === Socks5CommandType.CONNECT) {
                        ctx.pipeline().addLast(upstreamMatchHandler)
                        ctx.pipeline().remove(this)
                        ctx.fireChannelRead(socksRequest)
                    } else {
                        ctx.close()
                    }
                } else {
                    ctx.close()
                }
            }
            SocksVersion.UNKNOWN -> ctx.close()
            else -> ctx.close()
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.channel().closeOnFlush()
    }

    companion object {
        fun newInstance(upstreamMatchHandler: UpstreamMatchHandler): SocksServerHandler {
            return SocksServerHandler(upstreamMatchHandler)
        }
    }
}