package io.quckoo.client.http

import io.quckoo.client.QuckooClientV2

/**
  * Created by alonsodomin on 11/09/2016.
  */
object AjaxQuckooClient extends QuckooClientV2(new HttpDriver(AjaxHttpTransport))
