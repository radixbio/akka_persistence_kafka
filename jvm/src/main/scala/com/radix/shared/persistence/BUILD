package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "persistence-lib",
    srcs = glob(["*.scala"]),
    exports = [
        "@third_party//3rdparty/jvm/com/sksamuel/avro4s:avro4s_core",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_distributed_data",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_persistence",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_stream",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_stream_kafka",
        "@third_party//3rdparty/jvm/io/confluent:kafka_avro_serializer",
    ],
    deps = [
        "@third_party//3rdparty/jvm/com/sksamuel/avro4s:avro4s_core",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_distributed_data",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_persistence",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_stream",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_stream_kafka",
        "@third_party//3rdparty/jvm/io/confluent:kafka_avro_serializer",
        "@third_party//3rdparty/jvm/org/apache/avro",
        "@third_party//3rdparty/jvm/org/scalaz:scalaz_core",
    ],
)

scala_library(
    name = "persistence-query",
    srcs = glob(["query/**/*.scala"]),
    exports = [
        ":persistence-lib",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_persistence_query",
    ],
    deps = [
        ":persistence-lib",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_persistence_query",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_stream_kafka",
        "@third_party//3rdparty/jvm/io/confluent:kafka_avro_serializer",
    ],
)