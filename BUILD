
scala_library(
    name = "persistence-lib",
    srcs = glob(["src/main/scala/*.scala"]),
    resources = ["src/main/resources/reference.conf"],
    visibility = ["//shared/persistence:__subpackages__"],
    exports = [
        ":autoserz-lib",
        "@maven//:com_chuusai_shapeless_2_13",
        "@maven//:com_propensive_magnolia_2_13",
        "@maven//:com_propensive_mercator_2_13",
        "@maven//:com_sksamuel_avro4s_avro4s_core_2_13",
        "@maven//:com_typesafe_akka_akka_actor_2_13",
        "@maven//:com_typesafe_akka_akka_actor_typed_2_13",
        "@maven//:com_typesafe_akka_akka_distributed_data_2_13",
        "@maven//:com_typesafe_akka_akka_persistence_2_13",
        "@maven//:com_typesafe_akka_akka_stream_2_13",
        "@maven//:com_typesafe_akka_akka_stream_kafka_2_13",
        "@maven//:com_typesafe_config",
        "@maven//:io_confluent_kafka_avro_serializer",
        "@maven//:io_confluent_kafka_schema_registry_client",
        "@maven//:io_confluent_kafka_schema_serializer",
        "@maven//:org_apache_avro_avro",
        "@maven//:org_apache_kafka_kafka_clients",
        "@maven//:org_apache_kafka_kafka_server_common",
    ],
    deps = [
        ":autoserz-lib",
        "@maven//:com_chuusai_shapeless_2_13",
        "@maven//:com_propensive_magnolia_2_13",
        "@maven//:com_propensive_mercator_2_13",
        "@maven//:com_sksamuel_avro4s_avro4s_core_2_13",
        "@maven//:com_typesafe_akka_akka_actor_2_13",
        "@maven//:com_typesafe_akka_akka_actor_typed_2_13",
        "@maven//:com_typesafe_akka_akka_distributed_data_2_13",
        "@maven//:com_typesafe_akka_akka_persistence_2_13",
        "@maven//:com_typesafe_akka_akka_stream_2_13",
        "@maven//:com_typesafe_akka_akka_stream_kafka_2_13",
        "@maven//:com_typesafe_config",
        "@maven//:io_confluent_kafka_avro_serializer",
        "@maven//:io_confluent_kafka_schema_registry_client",
        "@maven//:io_confluent_kafka_schema_serializer",
        "@maven//:org_apache_avro_avro",
        "@maven//:org_apache_kafka_kafka_clients",
        "@maven//:org_apache_kafka_kafka_server_common",
    ],
)

scala_library(
    name = "persistence-query",
    srcs = glob(["src/main/scala/query/**/*.scala"]),
    visibility = ["//shared/persistence:__subpackages__"],
    exports = [
        ":persistence-lib",
        "@maven//:com_typesafe_akka_akka_persistence_query_2_13",
    ],
    deps = [
        ":persistence-lib",
        "@maven//:com_typesafe_akka_akka_actor_2_13",
        "@maven//:com_typesafe_akka_akka_persistence_query_2_13",
        "@maven//:com_typesafe_akka_akka_stream_kafka_2_13",
        "@maven//:io_confluent_kafka_avro_serializer",
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
        ":persistence-lib",
        "@maven//:com_typesafe_akka_akka_cluster_typed_2_13",
        "@maven//:com_typesafe_akka_akka_persistence_tck_2_13",
        "@maven//:com_typesafe_akka_akka_testkit_2_13",
        "@maven//:io_confluent_kafka_schema_registry",
        "@maven//:io_confluent_rest_utils",
        "@maven//:io_github_embeddedkafka_embedded_kafka_2_13",
        "@maven//:io_github_embeddedkafka_embedded_kafka_schema_registry_2_13",
        "@maven//:org_apache_kafka_kafka_2_13",
        "@maven//:org_apache_zookeeper_zookeeper",
        "@maven//:org_scalatest_scalatest_2_13",
    ],
)


scala_test(
    name = "avro-serialization-test",
    srcs = ["src/test/scala/ActorSerializationTest.scala"],
    deps = [
        ":persistence-lib",
        "@maven//:com_sksamuel_avro4s_avro4s_core_2_13",
        "@maven//:com_typesafe_akka_akka_actor_testkit_typed_2_13",
        "@maven//:com_typesafe_akka_akka_cluster_typed_2_13",
        "@maven//:com_typesafe_akka_akka_testkit_2_13",
        "@maven//:com_typesafe_config",
        "@maven//:org_scalatest_scalatest_2_13",
        "@maven//:org_scalatest_scalatest_shouldmatchers_2_13",
    ],
)

scala_test(
    name = "generic-serialization-test",
    srcs = [
        "src/test/scala/Test.scala",
    ],
    resources = ["src/test/resources/application.conf"],
    tags = ["exclusive"],
    deps = [
        ":persistence",
        "//test:test-lib",
        "@maven//:com_typesafe_akka_akka_actor_typed_2_13",
        "@maven//:io_github_embeddedkafka_embedded_kafka_schema_registry_2_13",
        "@maven//:org_scalatest_scalatest_2_13",
    ],
)

scala_library(
    name = "serialization-test",
    srcs = glob(["src/test/scala/SerializationTest.scala"]),
    visibility = ["//shared/persistence:__subpackages__"],
    exports = [
        "//test:test-lib",
        "@maven//:com_typesafe_akka_akka_actor_testkit_typed_2_13",
        "@maven//:com_typesafe_akka_akka_testkit_2_13",
        "@maven//:org_scalatest_scalatest_2_13",
    ],
    deps = [
        "//test:test-lib",
        "@maven//:com_typesafe_akka_akka_actor_testkit_typed_2_13",
        "@maven//:com_typesafe_akka_akka_testkit_2_13",
        "@maven//:org_scalatest_scalatest_2_13",
    ],
)


scala_binary(
    name = "auto-serializers",
    srcs = glob(["src/main/scala/autoserz/*.scala"]),
    main_class = "com.radix.shared.persistence.autoserz.AutoSerializers",
    visibility = ["//visibility:public"],
    deps = [
        "//shared/persistence:persistence-lib",
        "@maven//:com_lihaoyi_sourcecode_2_13",
        "@maven//:org_scalameta_common_2_13",
        "@maven//:org_scalameta_fastparse_v2_2_13",
        "@maven//:org_scalameta_parsers_2_13",
        "@maven//:org_scalameta_scalameta_2_13",
        "@maven//:org_scalameta_trees_2_13",
    ],
)

scala_binary(
    name = "auto-serializer-bindings",
    srcs = glob(["src/main/scala/autoserz/*.scala"]),
    main_class = "com.radix.shared.persistence.autoserz.AutoBindings",
    visibility = ["//visibility:public"],
    deps = [
        "//shared/persistence:persistence-lib",
        "@maven//:com_lihaoyi_sourcecode_2_13",
        "@maven//:org_scalameta_common_2_13",
        "@maven//:org_scalameta_fastparse_v2_2_13",
        "@maven//:org_scalameta_parsers_2_13",
        "@maven//:org_scalameta_scalameta_2_13",
        "@maven//:org_scalameta_trees_2_13",
    ],
)

scala_library(
    name = "autoserz-lib",
    srcs = ["src/main/scala/autoserz/AutoSerz.scala"],
)
