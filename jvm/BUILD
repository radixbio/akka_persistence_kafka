scala_library(
    name = "persistence",
    resources = ["src/main/resources/application.conf"],
    visibility = ["//visibility:public"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-query",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/dbpmjss/uservice:dbpmjss-uservice-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/gi/uservice:gi-uservice-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/hmrpp/uservice:hmrpp-uservice-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/sensors/elemental:elemental-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/util/prism:prism-serialization",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-query",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/dbpmjss/uservice:dbpmjss-uservice-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/gi/uservice:gi-uservice-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/hmrpp/uservice:hmrpp-uservice-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/sensors/elemental:elemental-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/util/prism:prism-serialization",
    ],
)

scala_test(
    name = "akka-persistence-tck",
    srcs = [
        "src/test/scala/KafkaJournalTest.scala",
        "src/test/scala/KafkaSnapshotStoreTest.scala",
        "src/test/scala/Test.scala",
    ],
    resources = ["src/test/resources/application.conf"],
    deps = [
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_cluster_typed",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_persistence_tck",
        "@third_party//3rdparty/jvm/io/github/embeddedkafka:embedded_kafka_schema_registry",
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
        "@third_party//3rdparty/jvm/com/propensive:magnolia",
    ],
)

scala_test(
    name = "avro-serialization-test",
    srcs = ["src/test/scala/SerializationTest.scala"],
    deps = [
        "@third_party//3rdparty/jvm/com/sksamuel/avro4s:avro4s_core",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_testkit_typed",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_cluster_typed",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_testkit",
        "//external:jar/com/typesafe/config",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
)