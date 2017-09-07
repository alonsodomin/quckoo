/*
 * Copyright 2015 A. Alonso Dominguez
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

package io.quckoo.console.layout

import io.quckoo.console.components.{NavStyle, TableStyle}

import scalacss.StyleSheet.Register
import scalacss.internal.StyleLookup

import CssSettings._

/**
  * Created by alonsodomin on 20/02/2016.
  */
final class LookAndFeel(implicit r: Register) extends StyleSheet.Inline()(r) {
  import ContextStyle._
  import dsl._

  val global  = Domain.ofValues(default, primary, success, info, warning, danger)
  val context = Domain.ofValues(success, info, warning, danger)

  def from[A](domain: Domain[A], base: String)(implicit lookup: StyleLookup[A]) = styleF(domain) {
    opt =>
      styleS(addClassNames(base, s"$base-$opt"))
  }

  def wrap(classNames: String*) = style(addClassNames(classNames: _*))

  val buttonOpt = from(global, "btn")
  val button    = buttonOpt(default)

  val panelOpt     = from(global, "panel")
  val panel        = panelOpt(default)
  val panelHeading = wrap("panel-heading")
  val panelBody    = wrap("panel-body")

  val labelOpt = from(global, "label")
  val label    = labelOpt(default)

  object nav {
    val domain =
      Domain.ofValues(NavStyle.tabs, NavStyle.pills, NavStyle.stacked)

    private val opt = from(domain, "nav")
    val tabs        = opt(NavStyle.tabs)
    val pills       = opt(NavStyle.pills)
    val stacked     = opt(NavStyle.stacked)

    def apply(navStyle: NavStyle.Value) = opt(navStyle)
  }

  object table {
    val domain = Domain.ofValues(
      TableStyle.bordered,
      TableStyle.condensed,
      TableStyle.hover,
      TableStyle.striped
    )

    private val opt = from(domain, "table")
    val base        = wrap("table")
    val bordered    = opt(TableStyle.bordered)
    val condensed   = opt(TableStyle.condensed)
    val hover       = opt(TableStyle.hover)
    val striped     = opt(TableStyle.striped)

    def apply(tableStyle: TableStyle.Value) = opt(tableStyle)
  }

  val alert = from(context, "alert")
  val close = wrap("close")

  object modal {
    val modal   = wrap("modal")
    val fade    = wrap("fade")
    val dialog  = wrap("modal-dialog")
    val content = wrap("modal-content")
    val header  = wrap("modal-header")
    val body    = wrap("modal-body")
    val footer  = wrap("modal-footer")
    val large   = wrap("modal-lg")
    val small   = wrap("modal-sm")
  }

  val formGroup   = wrap("form-group")
  val formControl = wrap("form-control")

  val controlLabel = wrap("control-label")

}
