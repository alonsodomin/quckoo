package io.quckoo.resolver.ivy

import java.io.File

/**
  * Created by domingueza on 03/11/2016.
  */
case class IvyConfig(baseDir: File,
                     resolutionDir: File,
                     repositoryDir: File,
                     ivyHome: Option[File])
