load(
    "@io_bazel_rules_scala//scala:scala.bzl",
    "scala_binary",
    "scala_library",
    "scala_macro_library",
    "scala_test",
    "scala_test_suite",
)

scala_library(
    name = "persistence",
    resources = ["src/main/resources/application.conf"],
    visibility = ["//visibility:public"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-query",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-query",
    ],
)

scala_test_suite(
    name = "akka-persistence-tck",
    deps = [
        "persistence-snapshot-tck",
        ":persistence-journal-tck",
    ],
)

scala_test(
    name = "persistence-journal-tck",
    srcs = [
        "src/test/scala/KafkaJournalTest.scala",
        "src/test/scala/Test.scala",
    ],
    resources = ["src/test/resources/application.conf"],
    deps = [
        "//3rdparty/jvm/com/typesafe/akka:akka_cluster_typed",
        "//3rdparty/jvm/com/typesafe/akka:akka_persistence_tck",
        "//3rdparty/jvm/io/github/embeddedkafka:embedded_kafka_schema_registry",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
)

scala_test(
    name = "persistence-snapshot-tck",
    srcs = [
        "src/test/scala/KafkaSnapshotStoreTest.scala",
        "src/test/scala/Test.scala",
    ],
    resources = ["src/test/resources/application.conf"],
    tags = ["exclusive"],
    deps = [
        "//3rdparty/jvm/com/typesafe/akka:akka_cluster_typed",
        "//3rdparty/jvm/com/typesafe/akka:akka_persistence_tck",
        "//3rdparty/jvm/io/github/embeddedkafka:embedded_kafka_schema_registry",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
)

scala_binary(
    name = "test_binary",
    srcs = ["src/test/scala/Test.scala"],
    main_class = "com.radix.shared.persistence.test.Test",
    resources = ["src/test/resources/application.conf"],
    deps = [
        ":persistence",
        "//3rdparty/jvm/com/propensive:magnolia",
    ],
)

scala_test(
    name = "avro-serialization-test",
    srcs = ["src/test/scala/SerializationTest.scala"],
    deps = [
        "//3rdparty/jvm/com/sksamuel/avro4s:avro4s_core",
        "//3rdparty/jvm/com/typesafe/akka:akka_actor_testkit_typed",
        "//3rdparty/jvm/com/typesafe/akka:akka_cluster_typed",
        "//3rdparty/jvm/com/typesafe/akka:akka_testkit",
        "//external:jar/com/typesafe/config",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
)
