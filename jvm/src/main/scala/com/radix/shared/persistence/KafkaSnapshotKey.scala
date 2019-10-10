package com.radix.shared.persistence

import akka.persistence.SnapshotMetadata

case class KafkaSnapshotKey(sequenceNr: Long, timestamp: Long) {
  override def toString: String =
    sequenceNr.toString
      .concat(",")
      .concat(timestamp.toString)
}

object KafkaSnapshotKey {
  def apply(key: String): KafkaSnapshotKey = {
    val kspace = key.split(',')
    KafkaSnapshotKey(kspace(0).toLong, kspace(1).toLong)
  }

  def apply(metadata: SnapshotMetadata): KafkaSnapshotKey = {
    KafkaSnapshotKey(metadata.sequenceNr, metadata.timestamp)
  }
}