package com.radix.shared.persistence.autoserz

import com.radix.shared.persistence.AvroSerializer

import java.nio.file.Paths
import scala.meta._
import scala.meta.contrib.XtensionExtractors

object AutoBindings {

  /**
   * Represents a class that may be involved in serialization
   */
  sealed trait SerializationObject

  sealed trait PathElement {
    val path: String
  }
  case class ObjectPath(path: String) extends PathElement
  case class ClassPath(path: String) extends PathElement

  implicit class FullPath(elements: Seq[PathElement]) {
    def formPathString: String = {
      elements
        .map {
          case ObjectPath(path) => path + "$"
          case ClassPath(path)  => path + "."
        }
        .mkString("")
        .stripSuffix(".")
    }
  }

  /**
   * Represents a serializer class, which extends [[AvroSerializer]]
   *
   * @param serializerName the name of the serializer class
   * @param serializedName the name of the class being serialized by this serializer
   * @param packageName the full package path of the serializer class
   */
  case class Serializer(serializerPath: List[PathElement], serializedPath: List[PathElement])
      extends SerializationObject {
    override def toString: String = {
      s"Serializer(${serializerPath.formPathString}, ${serializedPath.formPathString})"
    }
  }

  /**
   * Represents a class that could be serialized, but does not necessarily have a serializer
   *
   * @param serializedName the name of the class
   * @param packageName the full package path of the class
   */
  case class Serializable(serializedPath: List[PathElement]) extends SerializationObject {
    override def toString: String = {
      s"Serializable(${serializedPath.formPathString})"
    }
  }

  /**
   * Represents a binding between a class and its corresponding serializer
   *
   * @param serializerName the full class path and name of the serializer class
   * @param serializedName the full class path and name of the class being serialized by this serializer
   * @param bindingName the name to be used for the binding
   */
  case class SerializerBinding(serializerName: String, serializedName: String, bindingName: String)

  def main(args: Array[String]): Unit = {
    val parsedFiles: List[Source] = args.toList.map { path =>
      val file = Input.File(Paths.get(path))
      file.parse[Source].get
    }

    val serializationClasses = parsedFiles.flatMap(collectSerializationObjects)

    val serializerBindings = bindSerializationClasses(serializationClasses).sortBy(_.serializedName)

    val renderedBindings = renderSerializerBindings(serializerBindings)

    println(renderedBindings)
  }

  /**
   * Collects a list of [[SerializationObject SerializationObjects]] from the provided [[Source]]
   *
   * These [[SerializationObject SerializationObjects]] are either [[Serializer Serializers]] or
   * [[Serializable Serializables]]. Serializers represent classes that implement [[AvroSerializer]], and Serializables
   * represent classes that could be serialized but do not necessarily have a serializer. This is done because a class
   * may be serialized by a serializer that is not in the same file as the class itself, and so we collect all classes
   * that could possibly be serialized ahead of time.
   *
   * @param parsedFile the parsed [[Source]] file
   * @return a list of [[SerializationObject SerializationObjects]] found in the file
   */
  def collectSerializationObjects(parsedFile: Source): List[SerializationObject] = {
    val pkg = parsedFile
      .collect {
        case pkg: Pkg => pkg.ref.toString()
      }
      .headOption
      .getOrElse(throw new Exception("No package name found"))

    val classes = parsedFile.collect {
      case cls: Defn.Class                           => cls
      case trt: Defn.Trait                           => trt
      case obj: Defn.Object if obj.hasMod(mod"case") => obj
    }

    val serializers = classes
      .flatMap { cls =>
        for {
          extensionInit <- cls.templ.inits.headOption
          extension = extensionInit.tpe
          serializerName = cls.name.value

          // ensure that the class extended is "AvroSerializer"
          _ <- Some(extension).collect {
            case t"AvroSerializer[$_]" => ()
          }

          // get the list of generics of this extended class
          extendedClassGenerics <- extension.children.lift(1)
          // and take the first one, which is the class being serialized
          serializedClass <- extendedClassGenerics.children.headOption

          // split the class name from any preceding path
          serializedPath = serializedClass.toString().stripSuffix(".type").split("\\.").toList.map(ClassPath)

        } yield Serializer(
          pkg.split("\\.").toList.map(ClassPath) ++
            findLocalPath(cls, parsedFile) :+
            ClassPath(serializerName),
          serializedPath,
        )
      }

    val serializable = classes
      .map { cls =>
        Serializable(
          pkg.split("\\.").toList.map(ClassPath) ++
            findLocalPath(cls, parsedFile) :+
            (if (cls.isInstanceOf[Defn.Object]) ObjectPath(cls.name.value)
             else ClassPath(cls.name.value))
        )
      }
      .filterNot { serializable =>
        serializers.exists { serializer =>
          serializer.serializerPath.lastOption == serializable.serializedPath.lastOption
        }
      }

    serializable ++ serializers
  }

  /**
   * Creates a list of [[SerializerBinding SerializerBindings]] from a list of
   * [[SerializationObject SerializationObjects]], by matching up [[Serializer Serializers]] with the
   * [[Serializable Serializables]] that they serialize.
   *
   * @param serializationObjects the list of [[SerializationObject SerializationObjects]] to bind
   * @return a list of [[SerializerBinding SerializerBindings]]
   */
  def bindSerializationClasses(serializationObjects: List[SerializationObject]): List[SerializerBinding] = {
    val serializables = serializationObjects.collect {
      case serializable: Serializable => serializable
    }

    val serializers = serializationObjects.collect {
      case serializer: Serializer => serializer
    }

    serializers.flatMap { serializer =>
      val serializedClass = findSerializerBinding(serializer, serializables)

      serializedClass.map { serialized =>
        val serializerName = serializer.serializerPath.formPathString
        val serializedName = serialized.serializedPath.formPathString
        val bindingName = serialized.serializedPath.map(_.path).mkString("_") + "Serializer"

        SerializerBinding(
          serializerName,
          serializedName,
          bindingName,
        )
      }
    }
  }

  def findSerializerBinding(serializer: Serializer, serializables: List[Serializable]): Option[Serializable] = {
    serializables.foldLeft[Option[Serializable]](None) { (current, next) =>
      val currentMatchDepth = current
        .map { currentSerializable =>
          getMatchDepth(currentSerializable.serializedPath, serializer.serializedPath)
        }
        .getOrElse(0)

      val nextMatchDepth = getMatchDepth(next.serializedPath, serializer.serializedPath)

      if (nextMatchDepth > currentMatchDepth) {
        Some(next)
      } else {
        current
      }
    }
  }

  // todo: make these package objects into their own PathElement
  def getMatchDepth(p1: List[PathElement], p2: List[PathElement]): Int = {
    val path1 = p1.reverse.filterNot(_.path == "package")
    val path2 = p2.reverse.filterNot(_.path == "package")

    path1.zip(path2).count {
      case (l, r) =>
        l.path == r.path
    }
  }

  /**
   * Renders a list of [[SerializerBinding SerializerBindings]] as a HOCON config string.
   *
   * @param serializerBindings the list of [[SerializerBinding SerializerBindings]] to render
   * @return a HOCON config string
   */
  def renderSerializerBindings(serializerBindings: List[SerializerBinding]): String = {
    val serializers = serializerBindings
      .map {
        case SerializerBinding(serializerName, _, bindingName) =>
          s"""$bindingName = "$serializerName""""
      }
      .mkString("\n    ")

    val bindings = serializerBindings
      .map {
        case SerializerBinding(_, serializedName, bindingName) =>
          s""""$serializedName" = $bindingName"""
      }
      .mkString("\n    ")

    s"""
       |radix {
       |  serializers {
       |    $serializers
       |  }
       |
       |  serialization-bindings {
       |    $bindings
       |  }
       |}
       |""".stripMargin
  }

  /**
   * Finds the local path of a class, given the class itself and the [[Source]] file it is in.
   *
   * For example, if a class is contained in the object 'Bar', which is contained in the top-level object 'Foo', then
   * this function will return List("Foo", "Bar").
   *
   * @param term the class to find the path of
   * @param source the [[Source]] file the class is in
   * @return the local class path of the class
   */
  def findLocalPath(term: Member, source: Source): List[PathElement] = {
    val objects = source.collect {
      case obj: Defn.Object        => obj
      case pkgObj: meta.Pkg.Object => pkgObj
    }

    findAncestorObjects(term, objects)
      .flatMap { obj =>
        if (obj.isInstanceOf[meta.Pkg.Object]) {
          List(ClassPath(obj.name.value), ObjectPath("package"))
        } else {
          Some(ObjectPath(obj.name.value))
        }
      }
  }

  /**
   * Finds the list of ancestor objects of a class, given the class itself and a list of all objects in the file.
   *
   * @param term the class to find the ancestors of
   * @param objects the list of all objects in the file
   * @return the list of ancestor objects of the class
   */
  def findAncestorObjects(
    term: Member,
    objects: List[Member.Term with Stat.WithTemplate],
  ): List[Member.Term with Stat.WithTemplate] = {
    val parentOpt = objects.find { obj =>
      obj.templ.stats.contains(term)
    }

    parentOpt.toList.flatMap { parent =>
      findAncestorObjects(parent, objects) :+ parent
    }
  }
}
