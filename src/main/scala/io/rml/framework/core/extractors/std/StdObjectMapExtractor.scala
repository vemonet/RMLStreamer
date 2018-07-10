/*
 * Copyright (c) 2017 Ghent University - imec
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.rml.framework.core.extractors.std

import io.rml.framework.core.extractors.{JoinConditionExtractor, ObjectMapExtractor, TripleMapExtractor}
import io.rml.framework.core.model._
import io.rml.framework.core.model.rdf.{RDFLiteral, RDFResource}
import io.rml.framework.core.vocabulary.RMLVoc
import io.rml.framework.shared.RMLException

class StdObjectMapExtractor extends ObjectMapExtractor {
  /**
    * Extract.
    *
    * @param node Node to extract from.
    * @return
    */
  override def extract(node: RDFResource): List[ObjectMap] = {
    extractObjects(node) ::: extractObjectMaps(node)
  }

  /**
    * Extract objects. These are shortcuts for object maps with constants.
    * @param resource
    * @return
    */
  private def extractObjects(resource: RDFResource): List[ObjectMap] = {
    val property = RMLVoc.Property.OBJECT
    val properties = resource.listProperties(property)

    // iterates over predicates, converts these to predicate maps as blanks
    properties.map {
      case literal: RDFLiteral =>
        ObjectMap(Uri(""), constant = Some(Literal(literal.value)), termType = Some(Uri(RMLVoc.Class.LITERAL)))
      case resource: RDFResource =>
        ObjectMap(Uri(""), constant = Some(resource.uri), termType = Some(Uri(RMLVoc.Class.IRI)))
    }
  }

  /**
    * Extract object maps.
    * @param resource
    * @return
    */
  private def extractObjectMaps(resource: RDFResource): List[ObjectMap] = {
    val property = RMLVoc.Property.OBJECTMAP
    val properties = resource.listProperties(property)

    // iterates over predicatesMaps
    properties.flatMap {
      case literal: RDFLiteral =>
        throw new RMLException(literal.toString + ": Cannot convert literal to predicate map.")
      case resource: RDFResource => resource.getType match {
        case Some(Uri(RMLVoc.Class.FUNCTIONTERMMAP)) => None // TODO: rethink this
        case _ => Some(extractObjectMap(resource))
      }
    }

  }

  /**
    * Extract a single object map.
    * @param resource
    * @return
    */
  private def extractObjectMap(resource: RDFResource) : ObjectMap = {

    require(resource != null, "Resource can't be null.")

    val termType = extractTermType(resource)
    val template = extractTemplate(resource)
    val constant = extractConstant(resource)
    val reference = extractReference(resource)
    val parentTriplesMap = extractParentTriplesMap(resource)
    val joinCondition = extractJoinCondition(resource)
    val language = extractLanguage(resource)
    val datatype = extractDatatype(resource)

    ObjectMap(resource.uri, constant, reference, template, termType, datatype, language, parentTriplesMap, joinCondition)
  }

  def extractDatatype(resource: RDFResource) : Option[Uri] = {
    val property = RMLVoc.Property.DATATYPE
    val properties = resource.listProperties(property)

    if(properties.size > 1)
      throw new RMLException(resource.uri + ": invalid amount of reference properties.")
    if(properties.isEmpty) return None

    properties.head match {
      case literal: Literal => throw new RMLException(resource.uri + ": invalid data type.")
      case resource: RDFResource => Some(resource.uri)
    }
  }

  override def extractTermType(resource: RDFResource): Option[Uri] = {
    val result = super.extractTermType(resource)
    if(result.isDefined) result else {
      // if this is a reference-based term map or contains an referenceFormulation or has a datatype property the
      // term type is a literal
      val elements = resource.listProperties(RMLVoc.Property.REFERENCE) ++
        resource.listProperties(RMLVoc.Property.REFERENCEFORMULATION) ++
        resource.listProperties(RMLVoc.Property.DATATYPE)
      if(elements.nonEmpty) Some(Uri(RMLVoc.Class.LITERAL))
      else Some(Uri(RMLVoc.Class.IRI))
    }
  }

  def extractLanguage(resource: RDFResource): Option[Literal] = {
    val property = RMLVoc.Property.LANGUAGE
    val properties = resource.listProperties(property)

    if(properties.size > 1)
      throw new RMLException(resource.uri + ": invalid amount of language properties.")
    if(properties.isEmpty) return None

    properties.head match {
      case literal: Literal => Some(literal)
      case resource: RDFResource => throw new RMLException(resource.uri + ": invalid language type.")
    }
  }

  private def extractParentTriplesMap(resource: RDFResource) : Option[TripleMap] = {

    val property = RMLVoc.Property.PARENTTRIPLESMAP
    val properties = resource.listProperties(property)

    if(properties.size > 1)
      throw new RMLException(resource.uri + ": invalid amount of parent triple maps.")
    if(properties.isEmpty) return None

    properties.head match {
      case resource: RDFResource => TripleMapExtractor().extractTripleMapProperties(resource)
                                                        .flatMap(tm => Some(ParentTriplesMap(tm))) // transform to PTM
      case literal: Literal =>
        throw new RMLException(literal.toString + ": invalid parent triple map.")
    }

  }

  private def extractJoinCondition(resource: RDFResource) : Option[JoinCondition] = {
    val property = RMLVoc.Property.JOINCONDITION
    val properties = resource.listProperties(property)

    if(properties.size > 1)
      throw new RMLException(resource.uri + ": invalid amount of join conditions (amount=" + properties.size + ").")
    if(properties.isEmpty) return None

    properties.head match {
      case resource: RDFResource => JoinConditionExtractor().extract(resource)
      case literal: Literal =>
        throw new RMLException(literal.toString + ": invalid join condition, must be a resource.")
    }
  }
}
