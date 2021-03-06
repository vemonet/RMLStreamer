/**
  * MIT License
  *
  * Copyright (C) 2017 - 2020 RDF Mapping Language (RML)
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
  * THE SOFTWARE.
  *
  **/

package io.rml.framework.core.model

import java.util.Objects

import io.rml.framework.core.model.std.StdLogicalSource

/**
  * Info : ...
  */
trait LogicalSource extends Node {

  /**
    *
    * @return
    */
  def iterators: List[String]

  /**
    *
    * @return
    */
  def source: DataSource

  /**
    *
    * @return
    */
  def referenceFormulation: Uri


  /**
    * Logical sources are the same if their source identifier, their referenceFormulation and their iterator are
    * the same.
    *
    * @return
    */
  override def identifier: String = {
    Objects.hash(source.identifier, referenceFormulation.identifier, iterators).toHexString
  }

  /**
    * Logical sources are "semantically" the same if their source identifier and their referenceFormulation are
    * the same. This is used for grouping the sources, regardless of the iterators
    * @return
    */
  def semanticIdentifier: String = {
    Objects.hash(source.identifier, referenceFormulation.identifier).toHexString
  }

}

/**
  * Companion object for creating LogicalSource instances
  */
object LogicalSource {

  def apply(
             referenceFormulation: Uri,
             iterators: List[String],
             source: DataSource): LogicalSource = {

    StdLogicalSource(
      referenceFormulation,
      iterators,
      source)

  }

}
