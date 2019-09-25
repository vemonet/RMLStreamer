package io.rml.framework.util.server

trait StreamTestServerFactory {

  def createServer():TestServer
}

object TCPTestServerFactory extends StreamTestServerFactory {
  override def createServer(): TestServer = {
      TCPTestServer()
  }
}

object KafkaTestServerFactory extends StreamTestServerFactory{
  def createServer(topics: List[String]): TestServer = {
    KafkaTestServer(topics)
  }

  override def createServer():TestServer = {
    createServer(List("demo"))
  }
}

