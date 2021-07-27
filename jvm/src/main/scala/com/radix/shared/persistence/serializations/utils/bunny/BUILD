package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "bunny-serialization",
    srcs = [
        "Serializers.scala",
    ],
    plugins = ["//external:jar/org/typelevel/kind_projector_2_13_6"],
    resources = ["resources/reference.conf"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "@third_party//3rdparty/jvm/com/lihaoyi:ujson",
        "@third_party//3rdparty/jvm/io/circe:circe_parser",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "@third_party//3rdparty/jvm/com/lihaoyi:ujson",
        "@third_party//3rdparty/jvm/io/circe:circe_parser",
    ],
)
