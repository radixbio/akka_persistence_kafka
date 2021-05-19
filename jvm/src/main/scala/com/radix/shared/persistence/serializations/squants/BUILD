package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "squants-serializations",
    srcs = [
        "schemas.scala",
        "units.scala",
    ],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "@third_party//3rdparty/jvm/org/typelevel:squants",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "@third_party//3rdparty/jvm/org/typelevel:squants",
    ],
)

scala_library(
    name = "squants-circe-serialization",
    srcs = [
        "circeDerivations.scala",
    ],
    visibility = ["//visibility:public"],
    exports = [
        "//utils/rainbowuservice/jvm:rainbow-actor-protocol-lib",
        "@third_party//3rdparty/jvm/io/circe:circe_generic",
        "@third_party//3rdparty/jvm/io/circe:circe_generic_extras",
        "@third_party//3rdparty/jvm/io/circe:circe_parser",
        "@third_party//3rdparty/jvm/org/typelevel:squants",
    ],
    deps = [
        "@third_party//3rdparty/jvm/io/circe:circe_core",
        "@third_party//3rdparty/jvm/io/circe:circe_generic",
        "@third_party//3rdparty/jvm/io/circe:circe_generic_extras",
        "@third_party//3rdparty/jvm/io/circe:circe_parser",
        "@third_party//3rdparty/jvm/org/typelevel:squants",
    ],
)
