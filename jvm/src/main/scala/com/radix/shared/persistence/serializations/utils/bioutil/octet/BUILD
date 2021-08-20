package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "bioutil-octet-circe",
    srcs = [
        "defns.scala",
    ],
    exports = [
        "//shared/circe_custom_decoders:circe-custom-decoders",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-circe-serialization",
        "@third_party//3rdparty/jvm/io/circe:circe_generic",
        "@third_party//3rdparty/jvm/io/circe:circe_literal",
        "@third_party//3rdparty/jvm/org/typelevel:squants",
    ],
    deps = [
        "//shared/circe_custom_decoders:circe-custom-decoders",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-circe-serialization",
        "@third_party//3rdparty/jvm/io/circe:circe_generic",
        "@third_party//3rdparty/jvm/io/circe:circe_literal",
        "@third_party//3rdparty/jvm/org/typelevel:squants",
    ],
)
