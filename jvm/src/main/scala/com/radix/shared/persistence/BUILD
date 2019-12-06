load(
    "@io_bazel_rules_scala//scala:scala.bzl",
    "scala_binary",
    "scala_library",
    "scala_macro_library",
    "scala_test",
)

package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "persistence-lib",
    srcs = glob(["*.scala"]),
    exports = [
        "//3rdparty/jvm/com/sksamuel/avro4s:avro4s_core",
        "//3rdparty/jvm/com/typesafe/akka:akka_actor",
        "//3rdparty/jvm/com/typesafe/akka:akka_distributed_data",
        "//3rdparty/jvm/com/typesafe/akka:akka_persistence",
        "//3rdparty/jvm/com/typesafe/akka:akka_stream",
        "//3rdparty/jvm/com/typesafe/akka:akka_stream_kafka",
        "//3rdparty/jvm/io/confluent:kafka_avro_serializer",
    ],
    deps = [
        "//3rdparty/jvm/com/sksamuel/avro4s:avro4s_core",
        "//3rdparty/jvm/com/typesafe/akka:akka_actor",
        "//3rdparty/jvm/com/typesafe/akka:akka_distributed_data",
        "//3rdparty/jvm/com/typesafe/akka:akka_persistence",
        "//3rdparty/jvm/com/typesafe/akka:akka_stream",
        "//3rdparty/jvm/com/typesafe/akka:akka_stream_kafka",
        "//3rdparty/jvm/io/confluent:kafka_avro_serializer",
        "//3rdparty/jvm/org/apache/avro",
        "//3rdparty/jvm/org/scalaz:scalaz_core",
    ],
)

scala_library(
    name = "persistence-query",
    srcs = glob(["query/**/*.scala"]),
    exports = [
        ":persistence-lib",
        "//3rdparty/jvm/com/typesafe/akka:akka_persistence_query",
    ],
    deps = [
        ":persistence-lib",
        "//3rdparty/jvm/com/typesafe/akka:akka_actor",
        "//3rdparty/jvm/com/typesafe/akka:akka_persistence_query",
        "//3rdparty/jvm/com/typesafe/akka:akka_stream_kafka",
        "//3rdparty/jvm/io/confluent:kafka_avro_serializer",
    ],
)
