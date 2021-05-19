package com.radix.shared.persistence.serializations.device_drivers.eve

import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.{Schema, SchemaBuilder}

/**
 * A unit of measurement understood by Eve. Currently assuming that v1 and v2 use the same EveUnit definitions since
 * none were specified for v1.
 */
sealed trait EveUnit {

  /**
   * The String used to represent the EveUnit in JSON responses from Eve
   */
  val symbol: String

  /**
   * The alternative full name of the symbol that can be used in requests to Eve
   */
  val fullName: String
}

object EveUnit {

  case object Undefined extends EveUnit {
    override val symbol: String = "Undefined"
    override val fullName: String = "Undefined"
  }

  case object NoUnit extends EveUnit {
    override val symbol: String = "-"
    override val fullName: String = "None"
  }

  case object AmountOfCellsPerMilliliter extends EveUnit {
    override val symbol: String = "10^5 cells/ml"
    override val fullName = "AmountOfCellsPerMilliliter"
  }

  case object Bar extends EveUnit {
    override val symbol: String = "bar"
    override val fullName: String = "Bar"
  }

  case object Celsius extends EveUnit {
    override val symbol: String = "°C"
    override val fullName: String = "Celsius"
  }

  case object CellsPerMilliliter extends EveUnit {
    override val symbol: String = "cells/ml"
    override val fullName: String = "CellsPerMilliliter"
  }

  case object Day extends EveUnit {
    override val symbol: String = "d"
    override val fullName: String = "Day"
  }

  case object Degree extends EveUnit {
    override val symbol: String = "°"
    override val fullName: String = "Degree"
  }

  case object DegreePlato extends EveUnit {
    override val symbol: String = "°P"
    override val fullName: String = "DegreePlato"
  }

  case object Gram extends EveUnit {
    override val symbol: String = "g"
    override val fullName: String = "Gram"
  }

  case object GramPerGram extends EveUnit {
    override val symbol: String = "g/g"
    override val fullName: String = "GramPerGram"
  }

  case object GramPerGramPerHour extends EveUnit {
    override val symbol: String = "g/g/h"
    override val fullName: String = "GramPerGramPerHour"
  }

  case object GramPerHour extends EveUnit {
    override val symbol: String = "g/h"
    override val fullName: String = "GramPerHour"
  }

  case object GramPerHundredMilliliter extends EveUnit {
    override val symbol: String = "g/100ml"
    override val fullName: String = "GramPerHundredMilliliter"
  }

  case object GramPerKiloGram extends EveUnit {
    override val symbol: String = "g/kg"
    override val fullName: String = "GramPerKiloGram"
  }

  case object GramPerLiter extends EveUnit {
    override val symbol: String = "g/l"
    override val fullName: String = "GramPerLiter"
  }

  case object GramPerLiterCdw extends EveUnit {
    override val symbol: String = "g/l CDW"
    override val fullName: String = "GramPerLiterCdw"
  }

  case object GramPerMilliLiter extends EveUnit {
    override val symbol: String = "g/ml"
    override val fullName: String = "GramPerMilliLiter"
  }

  case object GramPerMinute extends EveUnit {
    override val symbol: String = "g/min"
    override val fullName: String = "GramPerMinute"
  }

  case object Hours extends EveUnit {
    override val symbol: String = "h"
    override val fullName: String = "Hours"
  }

  case object KiloGram extends EveUnit {
    override val symbol: String = "kg"
    override val fullName: String = "KiloGram"
  }

  case object KiloGramPerHour extends EveUnit {
    override val symbol: String = "kg/h"
    override val fullName: String = "KiloGramPerHour"
  }

  case object Liter extends EveUnit {
    override val symbol: String = "l"
    override val fullName: String = "Liter"
  }

  case object LiterPerHour extends EveUnit {
    override val symbol: String = "l/h"
    override val fullName: String = "LiterPerHour"
  }

  case object LiterPerMinute extends EveUnit {
    override val symbol: String = "l/min"
    override val fullName: String = "LiterPerMinute"
  }

  case object MicroGram extends EveUnit {
    override val symbol: String = "μg"
    override val fullName: String = "MicroGram"
  }

  case object MicroGramPerKilogram extends EveUnit {
    override val symbol: String = "μg/kg"
    override val fullName: String = "MicroGramPerKilogram"
  }

  case object MicroGramPerLiter extends EveUnit {
    override val symbol: String = "μg/l"
    override val fullName: String = "MicroGramPerLiter"
  }

  case object MicroGramPerMilliliter extends EveUnit {
    override val symbol: String = "μg/ml"
    override val fullName: String = "MicroGramPerMilliliter"
  }

  case object MicroLiter extends EveUnit {
    override val symbol: String = "μl"
    override val fullName: String = "MicroLiter"
  }

  case object MicroLiterPerLiter extends EveUnit {
    override val symbol: String = "μl/l"
    override val fullName: String = "MicroLiterPerLiter"
  }

  case object MicroLiterPerMinute extends EveUnit {
    override val symbol: String = "μl/min"
    override val fullName: String = "MicroLiterPerMinute"
  }

  case object MicroMolPerSquareMeterSecond extends EveUnit {
    override val symbol: String = "umol/(m^2*s)"
    override val fullName: String = "MicroMolPerSquareMeterSecond"
  }

  case object MilliBar extends EveUnit {
    override val symbol: String = "mbar"
    override val fullName: String = "MilliBar"
  }

  case object MilliGram extends EveUnit {
    override val symbol: String = "mg"
    override val fullName: String = "MilliGram"
  }

  case object MilliGramPerGram extends EveUnit {
    override val symbol: String = "mg/g"
    override val fullName: String = "MilliGramPerGram"
  }

  case object MilliGramPerHour extends EveUnit {
    override val symbol: String = "mg/h"
    override val fullName: String = "MilliGramPerHour"
  }

  case object MilliGramPerKiloGram extends EveUnit {
    override val symbol: String = "mg/kg"
    override val fullName: String = "MilliGramPerKiloGram"
  }

  case object MilliGramPerLiter extends EveUnit {
    override val symbol: String = "mg/l"
    override val fullName: String = "MilliGramPerLiter"
  }

  case object MilliGramPerMilliliter extends EveUnit {
    override val symbol: String = "mg/ml"
    override val fullName: String = "MilliGramPerMilliliter"
  }

  case object MilliLiter extends EveUnit {
    override val symbol: String = "ml"
    override val fullName: String = "MilliLiter"
  }

  case object MilliLiterPerHour extends EveUnit {
    override val symbol: String = "ml/h"
    override val fullName: String = "MilliLiterPerHour"
  }

  case object MilliLiterPerLiter extends EveUnit {
    override val symbol: String = "ml/l"
    override val fullName: String = "MilliLiterPerLiter"
  }

  case object MilliLiterPerMinute extends EveUnit {
    override val symbol: String = "ml/min"
    override val fullName: String = "MilliLiterPerMinute"
  }

  case object MilliMeter extends EveUnit {
    override val symbol: String = "mm"
    override val fullName: String = "MilliMeter"
  }

  case object MilliMolPerLiter extends EveUnit {
    override val symbol: String = "mmol/l"
    override val fullName: String = "MilliMolPerLiter"
  }

  case object MilliMolPerLiterPerHour extends EveUnit {
    override val symbol: String = "mmol/lh"
    override val fullName: String = "MilliMolPerLiterPerHour"
  }

  case object MilliSievertPerCentimeter extends EveUnit {
    override val symbol: String = "mS/cm"
    override val fullName: String = "MilliSievertPerCentimeter"
  }

  case object MilliVolt extends EveUnit {
    override val symbol: String = "mV"
    override val fullName: String = "MilliVolt"
  }

  case object Minutes extends EveUnit {
    override val symbol: String = "min"
    override val fullName: String = "Minutes"
  }

  case object MolPerHour extends EveUnit {
    override val symbol: String = "mol/h"
    override val fullName: String = "MolPerHour"
  }

  case object MolPerMol extends EveUnit {
    override val symbol: String = "mol/mol"
    override val fullName: String = "MolPerMol"
  }

  case object Percent extends EveUnit {
    override val symbol: String = "%"
    override val fullName: String = "Percent"
  }

  case object PercentO2 extends EveUnit {
    override val symbol: String = "%O2"
    override val fullName: String = "PercentO2"
  }

  case object PercentMassPerVolume extends EveUnit {
    override val symbol: String = "% m/v"
    override val fullName: String = "PercentMassPerVolume"
  }

  case object PercentVolumePerVolume extends EveUnit {
    override val symbol: String = "% v/v"
    override val fullName: String = "PercentVolumePerVolume"
  }

  case object PercentWeightPerWeight extends EveUnit {
    override val symbol: String = "% w/w"
    override val fullName: String = "PercentWeightPerWeight"
  }

  case object PerHour extends EveUnit {
    override val symbol: String = "1/h"
    override val fullName: String = "PerHour"
  }

  case object PerMinute extends EveUnit {
    override val symbol: String = "1/min"
    override val fullName: String = "PerMinute"
  }

  case object PerSecond extends EveUnit {
    override val symbol: String = "1/s"
    override val fullName: String = "PerSecond"
  }

  case object PicoFaradPerCentimeter extends EveUnit {
    override val symbol: String = "pF/cm"
    override val fullName: String = "PicoFaradPerCentimeter"
  }

  case object PoundByWeight extends EveUnit {
    override val symbol: String = "#"
    override val fullName: String = "PoundByWeight"
  }

  case object Seconds extends EveUnit {
    override val symbol: String = "s"
    override val fullName: String = "Seconds"
  }

  /**
   * Given the symbol or fullName of a unit as defined by Eve, return the appropriate EveUnit.
   */
  def parseEveUnit(str: String): EveUnit =
    str match {
      case Undefined.symbol | Undefined.fullName                                       => Undefined
      case NoUnit.symbol | NoUnit.fullName                                             => NoUnit
      case AmountOfCellsPerMilliliter.symbol | AmountOfCellsPerMilliliter.fullName     => AmountOfCellsPerMilliliter
      case Bar.symbol | Bar.fullName                                                   => Bar
      case Celsius.symbol | Celsius.fullName                                           => Celsius
      case CellsPerMilliliter.symbol | CellsPerMilliliter.fullName                     => CellsPerMilliliter
      case Day.symbol | Day.fullName                                                   => Day
      case Degree.symbol | Degree.fullName                                             => Degree
      case DegreePlato.symbol | DegreePlato.fullName                                   => DegreePlato
      case Gram.symbol | Gram.fullName                                                 => Gram
      case GramPerGram.symbol | GramPerGram.fullName                                   => GramPerGram
      case GramPerGramPerHour.symbol | GramPerGramPerHour.fullName                     => GramPerGramPerHour
      case GramPerHour.symbol | GramPerHour.fullName                                   => GramPerHour
      case GramPerHundredMilliliter.symbol | GramPerHundredMilliliter.fullName         => GramPerHundredMilliliter
      case GramPerKiloGram.symbol | GramPerKiloGram.fullName                           => GramPerKiloGram
      case GramPerLiter.symbol | GramPerLiter.fullName                                 => GramPerLiter
      case GramPerLiterCdw.symbol | GramPerLiterCdw.fullName                           => GramPerLiterCdw
      case GramPerMilliLiter.symbol | GramPerMilliLiter.fullName                       => GramPerMilliLiter
      case GramPerMinute.symbol | GramPerMinute.fullName                               => GramPerMinute
      case Hours.symbol | Hours.fullName                                               => Hours
      case KiloGram.symbol | KiloGram.fullName                                         => KiloGram
      case KiloGramPerHour.symbol | KiloGramPerHour.fullName                           => KiloGramPerHour
      case Liter.symbol | Liter.fullName                                               => Liter
      case LiterPerHour.symbol | LiterPerHour.fullName                                 => LiterPerHour
      case LiterPerMinute.symbol | LiterPerMinute.fullName                             => LiterPerMinute
      case MicroGram.symbol | MicroGram.fullName                                       => MicroGram
      case MicroGramPerKilogram.symbol | MicroGramPerKilogram.fullName                 => MicroGramPerKilogram
      case MicroGramPerLiter.symbol | MicroGramPerLiter.fullName                       => MicroGramPerLiter
      case MicroGramPerMilliliter.symbol | MicroGramPerMilliliter.fullName             => MicroGramPerMilliliter
      case MicroLiter.symbol | MicroLiter.fullName                                     => MicroLiter
      case MicroLiterPerLiter.symbol | MicroLiterPerLiter.fullName                     => MicroLiterPerLiter
      case MicroLiterPerMinute.symbol | MicroLiterPerMinute.fullName                   => MicroLiterPerMinute
      case MicroMolPerSquareMeterSecond.symbol | MicroMolPerSquareMeterSecond.fullName => MicroMolPerSquareMeterSecond
      case MilliBar.symbol | MilliBar.fullName                                         => MilliBar
      case MilliGram.symbol | MilliGram.fullName                                       => MilliGram
      case MilliGramPerGram.symbol | MilliGramPerGram.fullName                         => MilliGramPerGram
      case MilliGramPerHour.symbol | MilliGramPerHour.fullName                         => MilliGramPerHour
      case MilliGramPerKiloGram.symbol | MilliGramPerKiloGram.fullName                 => MilliGramPerKiloGram
      case MilliGramPerLiter.symbol | MilliGramPerLiter.fullName                       => MilliGramPerLiter
      case MilliGramPerMilliliter.symbol | MilliGramPerMilliliter.fullName             => MilliGramPerMilliliter
      case MilliLiter.symbol | MilliLiter.fullName                                     => MilliLiter
      case MilliLiterPerHour.symbol | MilliLiterPerHour.fullName                       => MilliLiterPerHour
      case MilliLiterPerLiter.symbol | MilliLiterPerLiter.fullName                     => MilliLiterPerLiter
      case MilliLiterPerMinute.symbol | MilliLiterPerMinute.fullName                   => MilliLiterPerMinute
      case MilliMeter.symbol | MilliMeter.fullName                                     => MilliMeter
      case MilliMolPerLiter.symbol | MilliMolPerLiter.fullName                         => MilliMolPerLiter
      case MilliMolPerLiterPerHour.symbol | MilliMolPerLiterPerHour.fullName           => MilliMolPerLiterPerHour
      case MilliSievertPerCentimeter.symbol | MilliSievertPerCentimeter.fullName       => MilliSievertPerCentimeter
      case MilliVolt.symbol | MilliVolt.fullName                                       => MilliVolt
      case Minutes.symbol | Minutes.fullName                                           => Minutes
      case MolPerHour.symbol | MolPerHour.fullName                                     => MolPerHour
      case MolPerMol.symbol | MolPerMol.fullName                                       => MolPerMol
      case Percent.symbol | Percent.fullName                                           => Percent
      case PercentO2.symbol | PercentO2.fullName                                       => PercentO2
      case PercentMassPerVolume.symbol | PercentMassPerVolume.fullName                 => PercentMassPerVolume
      case PercentVolumePerVolume.symbol | PercentVolumePerVolume.fullName             => PercentVolumePerVolume
      case PercentWeightPerWeight.symbol | PercentWeightPerWeight.fullName             => PercentWeightPerWeight
      case PerHour.symbol | PerHour.fullName                                           => PerHour
      case PerMinute.symbol | PerMinute.fullName                                       => PerMinute
      case PerSecond.symbol | PerSecond.fullName                                       => PerSecond
      case PicoFaradPerCentimeter.symbol | PicoFaradPerCentimeter.fullName             => PicoFaradPerCentimeter
      case PoundByWeight.symbol | PoundByWeight.fullName                               => PoundByWeight
      case Seconds.symbol | Seconds.fullName                                           => Seconds
      case _                                                                           => throw new IllegalArgumentException("Unit '" + str + "' does not correspond to any known Eve Unit!")
    }

  implicit object Schema extends SchemaFor[EveUnit] {
    override def schema: Schema =
      SchemaBuilder
        .builder("com.radix.shared.persistence.serializations.device_drivers.eve")
        .record("EveUnit")
        .fields()
        .requiredString("unit")
        .endRecord()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }

  implicit object AvroEncoder extends Encoder[EveUnit] {
    def encode(t: EveUnit): AnyRef = {
      val record = new GenericData.Record(schema)
      record.put("unit", t.fullName)
      record
    }
    override def schemaFor: SchemaFor[EveUnit] = Schema

  }

  implicit object AvroDecoder extends Decoder[EveUnit] {
    def decode(value: Any): EveUnit = {
      val record = value.asInstanceOf[GenericRecord]
      parseEveUnit(record.get("unit").toString)
    }
    override def schemaFor: SchemaFor[EveUnit] = Schema

  }

}
