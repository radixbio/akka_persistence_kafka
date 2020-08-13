package com.radix.shared.persistence.query

import akka.NotUsed
import akka.persistence.query.javadsl
import akka.stream.javadsl.Source

class KafkaJavaReadJournal(kafkaScalaReadJournal: KafkaScalaReadJournal)
    extends javadsl.ReadJournal
    with javadsl.CurrentPersistenceIdsQuery {

  override def currentPersistenceIds(): Source[String, NotUsed] = kafkaScalaReadJournal.currentPersistenceIds().asJava
}
