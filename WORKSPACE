workspace(
    name = "com_radix_shared_pekko_persistence_kafka",
)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "6.1"
RULES_JVM_EXTERNAL_SHA = "08ea921df02ffe9924123b0686dc04fd0ff875710bfadb7ad42badb931b0fd50"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/releases/download/%s/rules_jvm_external-%s.tar.gz" % (RULES_JVM_EXTERNAL_TAG, RULES_JVM_EXTERNAL_TAG)
)

http_archive(
    name = "bazel_skylib",
    sha256 = "cd55a062e763b9349921f0f5db8c3933288dc8ba4f76dd9416aac68acee3cb94",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/1.5.0/bazel-skylib-1.5.0.tar.gz",
        "https://github.com/bazelbuild/bazel-skylib/releases/download/1.5.0/bazel-skylib-1.5.0.tar.gz",
    ],
)


http_archive(
    name = "io_bazel_rules_scala",
    sha256 = "3b00fa0b243b04565abb17d3839a5f4fa6cc2cac571f6db9f83c1982ba1e19e5",
    strip_prefix = "rules_scala-6.5.0",
    url = "https://github.com/bazelbuild/rules_scala/releases/download/v6.5.0/rules_scala-v6.5.0.tar.gz",
)

## rules_jvm_external

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")

rules_jvm_external_deps()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

rules_jvm_external_setup()

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//:specs.bzl", "maven")

maven_install(
    artifacts = [
        "com.softwaremill.magnolia1_3:magnolia_3:1.3.4",
        "com.sksamuel.avro4s:avro4s-core_3:5.0.13",
        "com.typesafe:config:1.4.3",
        "org.apache.pekko:pekko-actor_2.13:1.0.2",
        "org.apache.pekko:pekko-actor-testkit-typed_3:1.0.2",
        "org.apache.pekko:pekko-actor-typed_2.13:1.0.2",
        "org.apache.pekko:pekko-cluster_2.13:1.0.2",
        "org.apache.pekko:pekko-cluster-typed_2.13:1.0.2",
        "org.apache.pekko:pekko-connectors-file_2.13:1.0.1",
        "org.apache.pekko:pekko-connectors-kafka_2.13:1.0.0",
        "org.apache.pekko:pekko-distributed-data_2.13:1.0.2",
        "org.apache.pekko:pekko-http-cors_2.13:1.1.0",
        "org.apache.pekko:pekko-persistence_2.13:1.0.2",
        "org.apache.pekko:pekko-persistence-query_2.13:1.0.2",
        "org.apache.pekko:pekko-persistence-testkit_2.13:1.0.2",
        "org.apache.pekko:pekko-persistence-typed_2.13:1.0.2",
        "org.apache.pekko:pekko-protobuf_2.13:1.0.2",
        "org.apache.pekko:pekko-remote_2.13:1.0.2",
        "org.apache.pekko:pekko-slf4j_2.13:1.0.2",
        "org.apache.pekko:pekko-stream_2.13:1.0.2",
        "org.apache.pekko:pekko-stream-typed_2.13:1.0.2",
        "org.apache.pekko:pekko-testkit_2.13:1.0.2",
        "org.apache.pekko:pekko-http_2.13:1.0.1",
        "org.apache.pekko:pekko-protobuf-v3_2.13:1.0.2",
        "org.apache.pekko:pekko-coordination_2.13:1.0.2",
        "io.confluent:kafka-avro-serializer:7.6.1",
        "io.confluent:kafka-schema-registry:7.6.1",
        "org.apache.avro:avro:1.11.3",
        "org.apache.kafka:kafka_2.13:3.6.2",
        "org.scalameta:scalameta_2.13:4.9.5",
        "org.scalatest:scalatest_3:3.2.18",
        "org.scalatest:scalatest-shouldmatchers_3:3.2.18",
        "org.slf4j:slf4j-api:2.0.13",
        "org.slf4j:log4j-over-slf4j:2.0.13",
        "org.lmdbjava:lmdbjava:0.7.0",
        maven.artifact(
            artifact = "embedded-kafka-schema-registry_2.13",
            exclusions = [
                maven.exclusion(
                    artifact = "slf4j_reload4j",
                    group = "org.slf4j",
                ),
            ],
            group = "io.github.embeddedkafka",
            version = "7.6.1",
        ),
        maven.artifact(
            testonly = True,
            artifact = "pekko-persistence-tck_2.13",
            group = "org.apache.pekko",
            version = "1.0.2",
        ),
    ],
    excluded_artifacts = [

    ],
    fetch_sources = True,
    generate_compat_repositories = True,
    maven_install_json = "//:maven_install.json",
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://packages.confluent.io/maven/",
    ],
    version_conflict_policy = "pinned",
)

load("@maven//:defs.bzl", "pinned_maven_install")

pinned_maven_install()  # lockfile for jvm

load("@io_bazel_rules_scala//:scala_config.bzl", "scala_config")

scala_config(
    enable_compiler_dependency_tracking = True,
    scala_version = "3.3.1",
)

load("@io_bazel_rules_scala//scala:scala.bzl", "rules_scala_setup", "rules_scala_toolchain_deps_repositories")

# loads other rules Rules Scala depends on
rules_scala_setup()

# Loads Maven deps like Scala compiler and standard libs. On production projects you should consider
# defining a custom deps toolchains to use your project libs instead
rules_scala_toolchain_deps_repositories(fetch_sources = True)

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

rules_proto_dependencies()

rules_proto_toolchains()

load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")

scala_register_toolchains()

load(
    "@io_bazel_rules_scala//scala/scalafmt:scalafmt_repositories.bzl",
    "scalafmt_default_config",
    "scalafmt_repositories",
)

scalafmt_default_config()

scalafmt_repositories()

load("@io_bazel_rules_scala//testing:scalatest.bzl", "scalatest_repositories", "scalatest_toolchain")

scalatest_repositories()

scalatest_toolchain()

#uncomment this for a ton of linter output
#register_toolchains("//tools:my_scala_toolchain")
