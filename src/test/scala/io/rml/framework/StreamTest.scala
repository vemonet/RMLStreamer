package io.rml.framework

import java.util.concurrent.Executors

import io.rml.framework.flink.source.StreamUtil
import org.apache.flink.api.common.io.OutputFormat
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.configuration.Configuration
import org.apache.flink.core.fs.FileSystem.WriteMode
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable.ArrayBuffer

class StreamTest extends FunSuite with Matchers {

  test("TCPSource - pull") {

    implicit val env = ExecutionEnvironment.getExecutionEnvironment
    implicit val senv = StreamExecutionEnvironment.getExecutionEnvironment

    val pool = Executors.newCachedThreadPool()

    // read the mapping
    val formattedMapping = TestUtil.readMapping("stream/stream-1.rml.ttl")

    // execute
    Main.createStreamFromFormattedMapping(formattedMapping).print() //TODO write to collection for assertions

    val server = new Runnable{
      override def run(): Unit = {
        val messages = List("{\"id\":2018}\n\r")
        TestUtil.createTCPServer(9999,messages.iterator)
      }
    }

    val job = new Runnable {
      override def run(): Unit = senv.execute()
    }

    pool.submit(server)
    Thread.sleep(2000)
    pool.submit(job)
    Thread.sleep(5000)

  }

}