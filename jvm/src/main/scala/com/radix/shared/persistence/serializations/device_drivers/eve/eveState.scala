package com.radix.shared.persistence.serializations.device_drivers.eve

/**
 * Represents the different states a batch can be in
 */
sealed trait EveState

object EveState {

  case object Completed extends EveState

  case object Planned extends EveState

  case object Running extends EveState

  def decodeFromString(str: String): EveState = {
    str match {
      case "Completed" => Completed
      case "Planned"   => Planned
      case "Running"   => Running
      case _           => throw new IllegalArgumentException(str + " not understood as valid Eve state!")
    }
  }

  def encodeToString(state: EveState): String = {
    state match {
      case Completed => "Completed"
      case Planned   => "Planned"
      case Running   => "Running"
    }
  }

}
