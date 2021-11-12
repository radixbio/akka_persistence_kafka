package com.radix.shared.persistence

import java.util.UUID

import akka.persistence.PersistentRepr

case class KafkaJournalKey(sequenceNr: Long, writerUuid: UUID, manifest: String, serializerId: Int) {
  override def toString: String =
    sequenceNr.toString
      .concat(",")
      .concat(writerUuid.toString)
      .concat(",")
      .concat(manifest)
      .concat(",")
      .concat(serializerId.toString)
}

object KafkaJournalKey {
  def apply(key: String): KafkaJournalKey = {
    val kspace = key.split(',')
    KafkaJournalKey(kspace(0).toLong, UUID.fromString(kspace(1)), kspace(2), kspace(3).toInt)
  }

  def apply(record: PersistentRepr, serializerId: Int): KafkaJournalKey = {
    KafkaJournalKey(
      record.sequenceNr,
      UUID.fromString(record.writerUuid),
      record.payload.getClass.getName,
      serializerId
    )
  }
}
