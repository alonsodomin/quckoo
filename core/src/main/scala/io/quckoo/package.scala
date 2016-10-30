/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io

import java.util.concurrent.Callable

/**
  * Created by aalonsodominguez on 07/07/15.
  */
package object quckoo {

  final val Logo =
    s"""
       |       ____             __
       |      / __ \\__  _______/ /______  ____
       |     / / / / / / / ___/ //_/ __ \\/ __ \\
       |    / /_/ / /_/ / /__/ ,< / /_/ / /_/ /
       |    \\___\\_\\__,_/\\___/_/|_|\\____/\\____/
       |                                        v${Info.version}
       |
     """.stripMargin

  type JobClass = Class[_ <: Callable[_]]

}
