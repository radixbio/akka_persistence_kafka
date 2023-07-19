package com.radix.shared.persistence.autoserz

import com.radix.shared.persistence.autoserz.AutoBindings.findLocalPath

import java.nio.file.Paths
import scala.meta.Stat.WithMods
import scala.meta._
import scala.meta.contrib.XtensionExtractors

object AutoSerializers {

  /**
   * A table of of the SchemaFor, Encoder, and Decoder imports associated with particular types. Should be expanded as
   * needed.
   *
   * When serializing case classes that use these types, these imports will be pulled in.
   *
   * These are used to speed up the serialization of case classes using common imports, for which we already have
   * schemas, encoders, and decoders. This avoids them having to be regenerated for every case class. These are not
   * required for serialization, but they do speed it up.
   */
  private val SEDImportTable: Map[String, ImportStatement] = Map(
    "Frequency" -> ImportStatement(
      "com.radix.shared.persistence.serializations.squants.schemas",
      Set("SchemaForFrequency", "FrequencyEncoder", "FrequencyDecoder"),
    ),
    "ImagePlus" -> ImportStatement(
      "com.radix.shared.persistence.serializations.media.images",
      Set("SchemaForImagePlus", "ImagePlusEncoder", "ImagePlusDecoder"),
    ),
    "Length" -> ImportStatement(
      "com.radix.shared.persistence.serializations.squants.schemas",
      Set("SchemaForLength", "LengthEncoder", "LengthDecoder"),
    ),
    "Mass" -> ImportStatement(
      "com.radix.shared.persistence.serializations.squants.schemas",
      Set("SchemaForMass", "MassEncoder", "MassDecoder"),
    ),
    "Path" -> ImportStatement(
      "com.radix.shared.persistence.PathSerializer",
      Set("SchemaForPath", "EncoderForPath", "DecoderForPath"),
    ),
    "RainbowMetaSerialized" -> ImportStatement(
      "com.radix.shared.persistence.serializations.utils.rainbow.ManualSerializers",
      Set("SerialRainbowS", "SerialRainbowE", "SerialRainbowD"),
    ),
    "RainbowModifyCommand" -> ImportStatement(
      "com.radix.shared.persistence.serializations.utils.rainbow.ManualSerializers",
      Set(
        "CasS",
        "CasE",
        "CasD",
        "InsertS",
        "InsertE",
        "InsertD",
        "RemoveS",
        "RemoveE",
        "RemoveD",
        "RelocateS",
        "RelocateE",
        "RelocateD",
        "ReplaceS",
        "ReplaceE",
        "ReplaceD",
        "MultiOpS",
        "MultiOpE",
        "MultiOpD",
      ),
    ),
    "Temperature" -> ImportStatement(
      "com.radix.shared.persistence.serializations.squants.schemas",
      Set("SchemaForTemp", "TemperatureEncoder", "TemperatureDecoder"),
    ),
    "Time" -> ImportStatement(
      "com.radix.shared.persistence.serializations.squants.schemas",
      Set("SchemaForTime", "TimeEncoder", "TimeDecoder"),
    ),
    "Velocity" -> ImportStatement(
      "com.radix.shared.persistence.serializations.squants.schemas",
      Set("SchemaForVelocity", "VelocityEncoder", "VelocityDecoder"),
    ),
    "Volume" -> ImportStatement(
      "com.radix.shared.persistence.serializations.squants.schemas",
      Set("SchemaForVolume", "VolumeEncoder", "VolumeDecoder"),
    ),
    "VolumeFlow" -> ImportStatement(
      "com.radix.shared.persistence.serializations.squants.schemas",
      Set("SchemaForVolumeFlow", "VolumeFlowEncoder", "VolumeFlowDecoder"),
    ),
  )

  case class ImportStatement(pkg: String, objects: Set[String]) {
    def render: String = {
      objects match {
        case set if set.isEmpty => s"import $pkg._"
        case _                  => s"import $pkg.{${objects.mkString(", ")}}"
      }
    }
  }

  def parseImportStatement(imp: Import): ImportStatement = {
    val pkg = imp.importers.head.ref.toString()
    val objects = imp.importers.flatMap(_.importees).map(_.toString()).toSet
    ImportStatement(pkg, objects)
  }

  /**
   * @param imports the set of packages to import in rendering the serializer object
   * @param serializers the list of avro serializer declarations
   */
  case class SerializerObject(imports: Set[ImportStatement], serializers: List[String]) {
    def render(objectName: String, packageName: String): String = {
      val baseImports = Set(
        ImportStatement("com.radix.shared.persistence", Set("AvroSerializer")),
        ImportStatement("com.sksamuel.avro4s", Set("Decoder", "Encoder", "SchemaFor")),
        ImportStatement("shapeless", Set("cachedImplicit")),
      )

      s"""package $packageName
         |
         |${(imports ++ baseImports).map(_.render).mkString("\n")}
         |
         |object $objectName {
         |  ${serializers.mkString("\n  ")}
         |}
         |""".stripMargin
    }

    def merge(other: SerializerObject): SerializerObject = {
      SerializerObject(
        imports ++ other.imports,
        serializers ++ other.serializers,
      )
    }
  }

  def main(args: Array[String]): Unit = {
    val parsedFiles: List[Source] = args.toList.dropRight(1).map { path =>
      val file = Input.File(Paths.get(path))
      file.parse[Source].get
    }

    val objectName = Paths.get(args.toList.last).getFileName.toString.split("\\.").head

    val packageName = parsedFiles.headOption
      .flatMap {
        _.collect {
          case pkg: Pkg => pkg.ref.toString()
        }.headOption
      }
      .getOrElse(throw new Exception("No package name found in source file"))

    val serializerObjects = parsedFiles.map { source =>
      val classes = findClassesToSerialize(source)
      val dependencies = collectExternalDependencies(classes)
      val dependencyImports = dependencies.flatMap(SEDImportTable.get)

      val needsEas = classes.map(_._2).exists(_.params.contains(ExtendedActorSystem))
      val easDependency = if (needsEas) {
        Set(
          ImportStatement(
            "akka.actor",
            Set("ExtendedActorSystem"),
          ),
          ImportStatement(
            "com.radix.shared.persistence.ActorRefSerializer",
            Set.empty,
          ),
        )
      } else {
        Set.empty
      }

      val packages = source
        .collect {
          case pkg: Pkg => pkg.ref.toString()
        }
      val packageImports = packages.toSet.map(ImportStatement(_, Set.empty))

      val existingImports = source.collect {
        case imp: Import => parseImportStatement(imp)
      }.toSet

      SerializerObject(
        dependencyImports ++ easDependency ++ packageImports ++ existingImports,
        classes.map {
          case (cls, annotation) =>
            renderSerializer(cls, annotation, source)
        },
      )
    }

    val serializerObject = serializerObjects.reduce(_ merge _)

    println(serializerObject.render(objectName, packageName))
  }

  def parseAutoSerzStat(stat: WithMods): AutoSerz = {
    stat.mods
      .filter(isAutoSerzMod)
      .collectFirst {
        case mod: Mod.Annot =>
          parseAutoSerzMod(mod)
      }
      .getOrElse(new AutoSerz)
  }

  def parseAutoSerzMod(mod: Mod.Annot): AutoSerz = {
    val params = mod.init.argClauses
      .flatMap(_.values)
      .collect {
        case term: Term.Name => AutoSerzParams.parse(term.value)
      }

    AutoSerz(params: _*)
  }

  def findClassesToSerialize(parsedFile: Source): List[(Member, AutoSerz)] = {
    val autoSerzClasses = parsedFile
      .collect {
        case cls: Defn.Class if !cls.hasMod(mod"abstract") => cls
        case trt: Defn.Trait if trt.hasMod(mod"sealed")    => trt
        case obj: Defn.Object if obj.hasMod(mod"case")     => obj
      }
      .flatMap { defn =>
        defn.mods
          .filter(isAutoSerzMod)
          .collectFirst {
            case mod: Mod.Annot =>
              (defn, parseAutoSerzMod(mod))
          }
      }

    val objects = parsedFile
      .collect {
        case obj: Defn.Object => obj
      }
      .filter { obj =>
        obj.mods.exists(isAutoSerzMod) && !obj.hasMod(Mod.Case())
      }

    val autoSerzObjectChildren = objects
      .flatMap { obj =>
        val autoSerz = parseAutoSerzStat(obj)

        obj.templ.stats
          .collect {
            case cls: Defn.Class if !cls.hasMod(mod"abstract") => cls
          }
          // If the class has its own AutoSerz annotation, use that instead of the object wide one
          .filterNot(_.mods.exists(isAutoSerzMod))
          .map { cls =>
            (cls, autoSerz)
          }
      }

    (autoSerzClasses ++ autoSerzObjectChildren).distinct
  }

  def isAutoSerzMod(mod: Mod): Boolean = {
    Some(mod)
      .collect {
        case annotation: Mod.Annot => annotation.init
      }
      .exists { init =>
        init.tpe match {
          case name: Type.Name => name.value == "AutoSerz"
          case _               => false
        }
      }
  }

  def collectExternalDependencies(classes: List[(Member, AutoSerz)]): Set[String] =
    classes
      .map(_._1)
      .flatMap {
        case cls: Defn.Class => cls.ctor.paramClauses.flatMap(_.values).flatMap(param => param.decltpe)
        case _               => Set.empty
      }
      .toSet
      .flatMap(findRawTypes)

  def findRawTypes(tpe: Type): Set[String] = {
    tpe match {
      case Type.Name(name)                 => Set(name)
      case Type.Apply.After_4_6_0(_, args) => args.flatMap(findRawTypes).toSet
      case Type.Tuple(args)                => args.flatMap(findRawTypes).toSet
      case Type.Repeated(tpe)              => findRawTypes(tpe)
      case Type.Select(_, name)            => findRawTypes(name)
      case _                               => Set.empty
    }
  }

  def renderSerializer(classToSerialize: Member, annotation: AutoSerz, source: Source): String = {
    val serializedLocalPath = findLocalPath(classToSerialize, source).map(_.path).mkString(".")
    val serializedName = s"$serializedLocalPath${classToSerialize.name.value}".replace(".", "")
    val serializedFullName = classToSerialize match {
      case _: Defn.Object => s"$serializedLocalPath.${classToSerialize.name.value}.type"
      case _              => s"$serializedLocalPath.${classToSerialize.name.value}"
    }

    val eas = if (annotation.params.contains(ExtendedActorSystem)) {
      "(implicit eas: ExtendedActorSystem)"
    } else {
      ""
    }

    val serializer = s"class ${serializedName}Avro$eas extends AvroSerializer[$serializedFullName]"
    lazy val schema = s"implicit val SchemaFor$serializedName: SchemaFor[$serializedFullName] = cachedImplicit"
    lazy val encoder = s"implicit val Encoder$serializedName: Encoder[$serializedFullName] = cachedImplicit"
    lazy val decoder = s"implicit val Decoder$serializedName: Decoder[$serializedFullName] = cachedImplicit"

    if (annotation.params.contains(CachedImplicits)) {
      List(serializer, schema, encoder, decoder).mkString("", "\n  ", "\n")
    } else serializer + "\n"
  }
}
