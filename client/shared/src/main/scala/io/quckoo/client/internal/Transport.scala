package io.quckoo.client.internal

import io.quckoo.client.internal.channel.ChannelFactory

/**
  * Created by alonsodomin on 05/09/2016.
  */
trait Transport[P <: Protocol] {

  def channelFor[R](implicit cf: ChannelFactory[P, R]): cf.Ch[cf.Out] = cf.channel(this)

}
