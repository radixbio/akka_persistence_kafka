package com.radix.shared.persistence.serializations.device_drivers.multitrons

import scodec.bits.BitVector

object defns {

  /**
   * The multitrons can be a one unit or three unit stack. This is how the internal docs talk about them. The different
   * units have different addresses for the serial communication protocol. Note that the units' addresses are not
   * necessarily the same as their order in a stack.
   *
   * The protocol mixes encodings, we we're just using an offset to work all in base ten here. That's the addressOffset.
   */
  sealed trait MultitronsUnit {
    val addressOffset: Int
  }

  case class MultitronsUpper() extends MultitronsUnit {
    override val addressOffset: Int = 0
  }

  case class MultitronsMiddle() extends MultitronsUnit {
    override val addressOffset: Int = 16
  }

  case class MultitronsLower() extends MultitronsUnit {
    override val addressOffset: Int = 32
  }

  /**
   * This is how we tell the device what to do and how we encode the serial protocol. We can read and set the Temperature,
   * Speed, Lights, Humidity, and CO2. Note that with reading from the device, we can't directly read from the serial
   * port. This can only be done with a callback method. Therefore we maintain state of the previous command set. We we
   * were to give the device commands too quickly, weird serial behavior could happen. However, the hardware would
   * probably get grumpy before the serial connection. I'd say don't send commands faster than every second or two which
   * is much faster than we need in real life.
   */
  sealed trait MultitronsCommand {
    val command: BitVector
    val unit: MultitronsUnit
  }

  /**
   * Creates a read command for a specific value from Multitron machine.
   *
   * @param commandId numerical id of the specific sensor
   * @param unit multitron unit
   * @return read command to get value from a specific sensor of a multitron
   */
  def readCommand(commandId: Int, unit: MultitronsUnit): BitVector = {

    BitVector(List(2, 48, 48, commandId + unit.addressOffset, 82, 65, 3).map(_.toByte))
  }

  /**
   * Creates a write command for a specific type of value (commandId) for a multitron unit,
   * writing value setValue.
   *
   * @param commandId id of the value to write
   * @param unit multitron unit
   * @param setValue value to be written
   * @return write command
   */
  def writeCommand(commandId: Int, unit: MultitronsUnit, setValue: Int): BitVector = {
    BitVector(List(2, 48, 48, commandId + unit.addressOffset, 82, 67, setValue, 3).map(_.toByte))
  }

  case class ReadTemperature(unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector = {
      readCommand(128, unit)
    }
  }

  case class ReadSpeed(unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      readCommand(129, unit)
  }

  case class ReadLights(unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      readCommand(130, unit)
  }

  case class ReadHumidity(unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      readCommand(131, unit)
  }

  case class ReadCO2(unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      readCommand(132, unit)
  }

  case class WriteTemperature(setTmp: Int, unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      writeCommand(128, unit, setTmp)
  }

  case class WriteSpeed(setSpeed: Int, unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      writeCommand(129, unit, setSpeed)
  }

  case class WriteLights(setLights: Boolean, unit: MultitronsUnit) extends MultitronsCommand {
    private[multitrons] implicit def boolToInt(boo: Boolean): Int = {
      boo match {
        case true  => 1
        case false => 0
      }
    }

    override val command: BitVector =
      writeCommand(130, unit, setLights)
  }

  case class WriteHumidity(setHumidity: Int, unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      writeCommand(131, unit, setHumidity)
  }

  case class WriteCO2(setCO2: Int, unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      writeCommand(132, unit, setCO2)
  }

}
