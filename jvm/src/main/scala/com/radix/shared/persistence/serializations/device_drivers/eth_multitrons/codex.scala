package com.radix.shared.persistence.serializations.device_drivers.eth_multitrons

import scala.annotation.tailrec

import scodec.bits.{BitVector, ByteVector}
import scodec.codecs.{float, uint16, uint32, uint8}
import scodec.{Attempt, DecodeResult, Decoder, Encoder, Err, SizeBound}
import squants.thermal.{Celsius, Temperature}
import squants.time.{Frequency, RevolutionsPerMinute}

import com.radix.shared.persistence.serializations.device_drivers.eth_multitrons.defns.{AbsoluteCommand, EMultitronsResponse}

object codex {
  private val CRCPoly: BitVector = BitVector.fromHex("A001").get
  private val CRCInit: BitVector = BitVector.high(16)

  def crc16(src: BitVector): BitVector = {
    src.toByteVector.foldLeft(CRCInit) { (crc, b) =>
      val rb = BitVector.fromByte(b).padLeft(16)
      val xcrc = crc ^ rb

      (0 until 8).foldLeft(xcrc) { (crci, s) =>
        if (crci.last) {
          (crci >>> 1) ^ CRCPoly
        } else {
          crci >>> 1
        }
      }
    }
  }

  def freqAsFloat(freq: Frequency): Float = {
    freq.toRevolutionsPerMinute.toFloat
  }
  def tempAsFloat(temp: Temperature): Float = {
    temp.toCelsiusDegrees.toFloat
  }

  def floatAsFreq(value: Float): Frequency = {
    RevolutionsPerMinute(value)
  }
  def floatAsTemp(value: Float): Temperature = {
    Celsius(value)
  }

  val CmdCoder: Encoder[AbsoluteCommand] = new Encoder[AbsoluteCommand] {
    override def encode(cmd: AbsoluteCommand): Attempt[BitVector] = cmd match {
      case write: defns.WriteCommand =>
        val value = float.encode(write.setValue).require.bytes
        val bitMap = (uint32.encode(1).require << write.parameterID).bytes

        val base = (ByteVector(2, 0, 1, 0, 17, 0x90, write.parameterID) ++ value ++ bitMap).bits

        val crc = crc16(base)

        Attempt.Successful(base ++ crc)
      case read: defns.ReadCommand =>
        val base = ByteVector(2, 0, read.parameterID, 0, 8, 0x92).bits
        val crc = crc16(base)

        Attempt.Successful(base ++ crc)
    }

    override def sizeBound: SizeBound = SizeBound.atLeast(8)
  }

  val RespDecoder: Decoder[EMultitronsResponse] = (bits: BitVector) => {
    println(s"Decoding raw response: ${bits.toHex}")

    bits.acquireThen(6 * 8)(
      { _ =>
        Attempt.Successful(DecodeResult(defns.ErrorOccurred("Insufficient bits in response packet"), bits))
      },
      { headbits =>
        val header = headbits.toByteArray

        println(s"Header: ${header.map(_.toHexString).mkString("Array(", ", ", ")")}")

        val startChar = header.head
        val length = uint8.decode(bits.slice(48, 56)).require.value
        val cmd = header(5)

        println(s"Start Char: $startChar | Len: $length | CMD: $cmd")

        if (startChar == 2) {
          bits
            .drop(6 * 8)
            .acquireThen(length * 8)(
              { _ =>
                Attempt.Successful(DecodeResult(defns.ErrorOccurred("Insufficient bits in response packet"), bits))
              },
              { bodybits =>
                val tbodybits = bodybits.dropRight(16)

                if (cmd == 0x90.byteValue) {
                  WriteConfDec.decode(tbodybits) match {
                    case s: Attempt.Successful[DecodeResult[defns.WriteConfirmation]] => s
                    case Attempt.Failure(cause) =>
                      Attempt.Successful(DecodeResult(defns.ErrorOccurred(cause.message), tbodybits))
                  }
                } else if (cmd == 0x92.byteValue) {
                  ValueRespDec.decode(tbodybits) match {
                    case s: Attempt.Successful[DecodeResult[defns.ValueResponse]] => s
                    case Attempt.Failure(cause) =>
                      Attempt.Successful(DecodeResult(defns.ErrorOccurred(cause.message), tbodybits))
                  }
                } else {
                  Attempt
                    .Successful(DecodeResult(defns.ErrorOccurred("Invalid command ID in response packet"), tbodybits))
                }
              }
            )
        } else {
          Attempt.Successful(DecodeResult(defns.ErrorOccurred("Malformed response packet"), bits))
        }
      }
    )
  }

  val WriteConfDec: Decoder[defns.WriteConfirmation] = (bits: BitVector) => {
    Attempt.Successful(DecodeResult(defns.WriteConfirmation(), BitVector.empty))
  }

  val ValueRespDec: Decoder[defns.ValueResponse] = (bits: BitVector) => {
    val byteData = bits.toByteArray
    println("Body: " + bits.toHex)
    if (byteData.length > 32) {
      val relBytes = BitVector(byteData.slice(28, 32))

      val value = float.decode(relBytes).require.value

      Attempt.Successful(DecodeResult(defns.ValueResponse(value), BitVector.empty))
    } else {
      Attempt.Failure(Err("Insufficient bytes in value response"))
    }
  }
}
