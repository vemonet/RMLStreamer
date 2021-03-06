package io.rml.framework.flink.util

import io.rml.framework.flink.util.ParameterUtil.OutputSinkOption.OutputSinkOption
import io.rml.framework.flink.util.ParameterUtil.PostProcessorOption.PostProcessorOption

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
object ParameterUtil {

  case class ParameterConfig(
                              mappingFilePath: String = ".",
                              jobName: String = "RMLStreamer",
                              baseIRI: Option[String] = None,
                              localParallel: Boolean = true,
                              postProcessor: PostProcessorOption = PostProcessorOption.None,
                              checkpointInterval: Option[Long] = None,
                              outputPath: Option[String] = None,
                              brokerList: Option[String] = None,
                              topic: Option[String] = None,
                              partitionId: Option[Int] = None,
                              socket: Option[String] = None,
                              outputSink: OutputSinkOption = OutputSinkOption.File
                            ) {
    override def toString: String = {
      val resultStr: String =
        s"Mapping file: ${mappingFilePath}\n" +
        s"Job name: ${jobName}\n" +
        s"Base IRI: ${baseIRI.getOrElse("/")}\n" +
        s"Parallelise over local task slots: ${localParallel}\n" +
        s"Post processor: ${postProcessor}\n" +
        s"Checkpoint interval: ${checkpointInterval.getOrElse("/")}\n" +
        s"Output method: ${outputSink}\n" +
        s"Output file: ${outputPath.getOrElse("/")}\n" +
        s"Kafka broker list: ${brokerList.getOrElse("/")}\n" +
        s"Kafka topic: ${topic.getOrElse("/")}\n" +
        s"Kafka topic partition id: ${partitionId.getOrElse("/")}\n" +
        s"Output TCP socket: ${socket.getOrElse("/")}\n" +
      s"Discard output: ${outputSink.equals(OutputSinkOption.None)}"
      resultStr
    }
  }

  // possible output sink options
  object OutputSinkOption extends Enumeration {
    type OutputSinkOption = Value
    val File, Socket, Kafka, None = Value
  }

  // possible post processor options
  object PostProcessorOption extends Enumeration {
    type PostProcessorOption = Value
    val None, Bulk, JsonLD = Value
  }


  val parser = new scopt.OptionParser[ParameterConfig]("RMLStreamer") {
    override def showUsageOnError = true

    head("RMLStreamer", "2.0.0")

    opt[String]('j', "job-name").valueName("<job name>")
      .optional()
      .action((value, config) => config.copy(jobName = value))
      .text("The name to assign to the job on the Flink cluster. Put some semantics in here ;)")

    opt[String]('i', "base-iri").valueName("<base IRI>")
      .optional()
      .action((value, config) => config.copy(baseIRI = Some(value)))
      .text("The base IRI as defined in the R2RML spec.")

    opt[Unit]("disable-local-parallel")
      .optional()
      .action((_, config) => config.copy(localParallel = false))
      .text("By default input records are spread over the available task slots within a task manager to optimise parallel processing," +
        "at the cost of losing the order of the records throughout the process." +
        " This option disables this behaviour to guarantee that the output order is the same as the input order.")

    opt[String]('m', "mapping-file").valueName("<RML mapping file>").required()
      .action((value, config) => config.copy(mappingFilePath = value))
      .text("REQUIRED. The path to an RML mapping file. The path must be accessible on the Flink cluster.")

    opt[Unit]("json-ld")
        .optional()
        .action((_, config) => config.copy(postProcessor = PostProcessorOption.JsonLD))
        .text("Write the output as JSON-LD instead of N-Quads. An object contains all RDF generated from one input record. " +
          "Note: this is slower than using the default N-Quads format.")

    opt[Unit]("bulk")
      .optional()
      .action((_, config) => config.copy(postProcessor = PostProcessorOption.Bulk))
      .text("Write all triples generated from one input record at once.")

    opt[Long]("checkpoint-interval").valueName("<time (ms)>")
        .optional()
        .action((value, config) => config.copy(checkpointInterval = Some(value)))
        .text("If given, Flink's checkpointing is enabled with the given interval. If not given, checkpointing is disabled.")

    // options specifically for writing output to file
    cmd("toFile")
      .text("Write output to file")
      .action((_, config) => config.copy(outputSink = OutputSinkOption.File))
      .children(
        opt[String]('o', "output-path").valueName("<output file>").required()
          .action((value, config) => config.copy(outputPath = Some(value)))
          .text("The path to an output file.")
      )

    // options specifically for writing output to a Kafka topic
    cmd("toKafka")
      .text("Write output to a Kafka topic")
      .action((_, config) => config.copy(outputSink = OutputSinkOption.Kafka))
      .children(
        opt[String]('b', "broker-list").valueName("<host:port>[,<host:port>]...").required()
          .action((value, config) => config.copy(brokerList = Some(value)))
          .text("A comma separated list of Kafka brokers."),
        opt[String]('t', "topic").valueName("<topic name>").required()
          .action((value, config) => config.copy(topic = Some(value)))
          .text("The name of the Kafka topic to write output to."),
        opt[Int]("partition-id").valueName("<id>").optional()
          .text("EXPERIMENTAL. The partition id of kafka topic to which the output will be written to.")
          .action((value, config) => config.copy(partitionId = Some(value)))
      )

    // options specifically for writing to TCP socket
    cmd("toTCPSocket")
      .text("Write output to a TCP socket")
      .action((_, config) => config.copy(outputSink = OutputSinkOption.Socket))
      .children(
        opt[String]('s', "output-socket").valueName("<host:port>").required()
          .action((value, config) => config.copy(socket = Some(value)))
          .text("The TCP socket to write to.")
      )

    cmd("noOutput")
      .text("Do everything, but discard output")
      .action((_, config) => config.copy(outputSink = OutputSinkOption.None))
  }

  def processParameters(args: Array[String]): Option[ParameterConfig] = {
    parser.parse(args, ParameterConfig())
  }

}
