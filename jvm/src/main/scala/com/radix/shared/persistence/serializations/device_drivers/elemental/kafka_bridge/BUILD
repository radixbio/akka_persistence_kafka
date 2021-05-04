package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "elemental-bridge-serializations",
    srcs = [
        "defns.scala",
    ],
    resources = [],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "@third_party//3rdparty/jvm/io/circe:circe_core",
        "@third_party//3rdparty/jvm/org/apache/kafka:connect_api",
        "@third_party//3rdparty/jvm/org/apache/kafka:connect_runtime",
        "@third_party//3rdparty/jvm/org/apache/kafka:connect_json"
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "@third_party//3rdparty/jvm/io/circe:circe_core",
        "@third_party//3rdparty/jvm/org/apache/kafka:connect_api",
        "@third_party//3rdparty/jvm/org/apache/kafka:connect_runtime",
        "@third_party//3rdparty/jvm/org/apache/kafka:connect_json"
    ],
)
